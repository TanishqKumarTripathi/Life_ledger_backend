package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.*;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository transactionRepository;

    private static final List<String> COLORS = List.of(
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444",
            "#8B5CF6", "#EC4899", "#06B6D4", "#F43F5E");

    @Override
    public DashboardResponseDTO getDashboard(Long userId, Long accountId) {

        List<Transaction> txns = transactionRepository.findAllByBankAccountId(accountId);

        return DashboardResponseDTO.builder()
                .monthlyTimeline(buildMonthlyTimeline(txns))
                .categories(buildCategoryBreakdown(txns))
                .merchants(buildMerchantBreakdown(txns))
                .recurringVsOneTime(buildRecurringSplit(txns))
                .burnRate(buildBurnRate(txns))
                .yearOverYear(buildMonthlyTimeline(txns))
                .build();
    }

    /* ---------------- MONTHLY TIMELINE ---------------- */
    private List<MonthlyInsightDto> buildMonthlyTimeline(List<Transaction> txns) {

        Map<String, Double> result = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();

        for (int i = 11; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            String monthName = m.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            double total = txns.stream()
                    .filter(t -> t.getDate().getMonthValue() == m.getMonthValue()
                            && t.getDate().getYear() == m.getYear())
                    .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                    .sum();

            result.put(monthName, total);
        }

        return result.entrySet().stream()
                .map(e -> new MonthlyInsightDto(e.getKey(), e.getValue()))
                .toList();
    }

    /* ---------------- CATEGORY BREAKDOWN ---------------- */
    private List<CategoryInsightDto> buildCategoryBreakdown(List<Transaction> txns) {

        Map<String, CategoryInsightDto> map = new HashMap<>();

        for (Transaction t : txns) {
            String category = (t.getCategory() != null)
                    ? t.getCategory().getName()
                    : "Uncategorized";

            map.putIfAbsent(category, new CategoryInsightDto(category, 0, 0, null));

            CategoryInsightDto dto = map.get(category);
            dto.setAmount(dto.getAmount() + Math.abs(t.getAmount().doubleValue()));
            dto.setCount(dto.getCount() + 1);
        }

        List<CategoryInsightDto> sorted = map.values().stream()
                .sorted(Comparator.comparingDouble(CategoryInsightDto::getAmount).reversed())
                .limit(6)
                .toList();

        // Assign colors so donut chart works
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setColor(COLORS.get(i % COLORS.size()));
        }

        return sorted;
    }

    /* ---------------- MERCHANT BREAKDOWN ---------------- */
    private List<MerchantInsightDto> buildMerchantBreakdown(List<Transaction> txns) {

        Map<String, MerchantInsightDto> map = new HashMap<>();

        for (Transaction t : txns) {
            String merchant = (t.getMerchant() == null || t.getMerchant().isBlank())
                    ? "Unknown"
                    : t.getMerchant().split(" ")[0];

            map.putIfAbsent(merchant, new MerchantInsightDto(merchant, 0, 0));

            MerchantInsightDto dto = map.get(merchant);
            dto.setAmount(dto.getAmount() + Math.abs(t.getAmount().doubleValue()));
            dto.setCount(dto.getCount() + 1);
        }

        return map.values().stream()
                .sorted(Comparator.comparingDouble(MerchantInsightDto::getAmount).reversed())
                .limit(8)
                .toList();
    }

    /* ---------------- RECURRING VS ONE-TIME ---------------- */
    private RecurringVsOneTimeDto buildRecurringSplit(List<Transaction> txns) {

        Map<String, Long> merchantFreq = txns.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchant, Collectors.counting()));

        double recurringTotal = 0;
        double oneTimeTotal = 0;

        for (Transaction t : txns) {
            long count = merchantFreq.getOrDefault(t.getMerchant(), 1L);
            double amt = Math.abs(t.getAmount().doubleValue());

            if (count >= 3)
                recurringTotal += amt;
            else
                oneTimeTotal += amt;
        }

        return new RecurringVsOneTimeDto(recurringTotal, oneTimeTotal);
    }

    /* ---------------- BURN RATE ---------------- */
    private BurnRateDto buildBurnRate(List<Transaction> txns) {

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        double monthlySpend = txns.stream()
                .filter(t -> t.getDate().getMonthValue() == month &&
                        t.getDate().getYear() == year)
                .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                .sum();

        int day = now.getDayOfMonth();
        int daysInMonth = now.lengthOfMonth();
        int daysLeft = daysInMonth - day;

        double daily = day > 0 ? monthlySpend / day : 0;
        double projected = daily * daysInMonth;

        return BurnRateDto.builder()
                .current(monthlySpend)
                .daily(daily)
                .projected(projected)
                .daysLeft(daysLeft)
                .build();
    }

    @Override
    public AnalyticsDTO getLatestAnalytics(Long userId) {
        List<Transaction> txns = transactionRepository.findAllByUserId(userId);

        AnalyticsDTO analytics = new AnalyticsDTO();
        analytics.setMonthlyTimeline(buildMonthlyTimelineData(txns));
        analytics.setBurnRate(buildBurnRateData(txns));
        analytics.setTopCategories(buildTopCategoriesData(txns));
        analytics.setTopMerchants(buildTopMerchantsData(txns));
        analytics.setSpendingTypes(buildSpendingTypesData(txns));
        analytics.setAverages(buildAveragesData(txns));
        analytics.setYearOverYear(buildYearOverYearData(txns));

        return analytics;
    }

    private List<AnalyticsDTO.MonthlyTimelineData> buildMonthlyTimelineData(List<Transaction> txns) {
        return buildMonthlyTimeline(txns).stream()
                .map(m -> {
                    AnalyticsDTO.MonthlyTimelineData data = new AnalyticsDTO.MonthlyTimelineData();
                    data.setMonth(m.getMonth());
                    data.setExpenses(m.getAmount());
                    data.setIncome(0.0);
                    data.setSavings(0.0);
                    return data;
                })
                .toList();
    }

    private AnalyticsDTO.BurnRateData buildBurnRateData(List<Transaction> txns) {
        BurnRateDto burnRate = buildBurnRate(txns);
        AnalyticsDTO.BurnRateData data = new AnalyticsDTO.BurnRateData();
        data.setCurrentRate(burnRate.getDaily());
        data.setTrend("stable");
        data.setDaysToZero((int) burnRate.getDaysLeft());
        return data;
    }

    private List<AnalyticsDTO.CategoryData> buildTopCategoriesData(List<Transaction> txns) {
        return buildCategoryBreakdown(txns).stream()
                .map(c -> {
                    AnalyticsDTO.CategoryData data = new AnalyticsDTO.CategoryData();
                    data.setCategory(c.getName());
                    data.setAmount(c.getAmount());
                    data.setPercentage(0.0);
                    return data;
                })
                .toList();
    }

    private List<AnalyticsDTO.MerchantData> buildTopMerchantsData(List<Transaction> txns) {
        return buildMerchantBreakdown(txns).stream()
                .map(m -> {
                    AnalyticsDTO.MerchantData data = new AnalyticsDTO.MerchantData();
                    data.setMerchant(m.getName());
                    data.setAmount(m.getAmount());
                    data.setTransactions((int) m.getCount());
                    data.setBankAccount("");
                    return data;
                })
                .toList();
    }

    private AnalyticsDTO.SpendingTypeData buildSpendingTypesData(List<Transaction> txns) {
        RecurringVsOneTimeDto split = buildRecurringSplit(txns);
        AnalyticsDTO.SpendingTypeData data = new AnalyticsDTO.SpendingTypeData();
        data.setRecurring(split.getRecurring());
        data.setOneTime(split.getOneTime());
        double total = split.getRecurring() + split.getOneTime();
        data.setRecurringPercentage(total > 0 ? (split.getRecurring() / total) * 100 : 0);
        data.setOneTimePercentage(total > 0 ? (split.getOneTime() / total) * 100 : 0);
        return data;
    }

    private AnalyticsDTO.AveragesData buildAveragesData(List<Transaction> txns) {
        double totalAmount = txns.stream().mapToDouble(t -> Math.abs(t.getAmount().doubleValue())).sum();
        int days = 30;
        AnalyticsDTO.AveragesData data = new AnalyticsDTO.AveragesData();
        data.setDailySpending(totalAmount / days);
        data.setWeeklySpending(totalAmount / 4);
        data.setMonthlySpending(totalAmount);
        return data;
    }

    private AnalyticsDTO.YearOverYearData buildYearOverYearData(List<Transaction> txns) {
        LocalDate now = LocalDate.now();
        double currentYear = txns.stream()
                .filter(t -> t.getDate().getYear() == now.getYear())
                .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                .sum();
        double previousYear = txns.stream()
                .filter(t -> t.getDate().getYear() == now.getYear() - 1)
                .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                .sum();

        AnalyticsDTO.YearOverYearData data = new AnalyticsDTO.YearOverYearData();
        data.setCurrentYearTotal(currentYear);
        data.setPreviousYearTotal(previousYear);
        data.setChangePercentage(previousYear > 0 ? ((currentYear - previousYear) / previousYear) * 100 : 0);
        data.setTrend(currentYear > previousYear ? "increasing" : "decreasing");
        return data;
    }

    @Override
    public DashboardStatsDto getDashboardStats(Long userId) {
        List<Transaction> txns = transactionRepository.findAllByUserId(userId);
        double totalSpent = txns.stream().mapToDouble(t -> Math.abs(t.getAmount().doubleValue())).sum();

        return DashboardStatsDto.builder()
                .totalSpent(totalSpent)
                .totalIncome(0.0)
                .budgetLeft(0.0)
                .transactionCount((long) txns.size())
                .subscriptionCount(0L)
                .savingsRate(0.0)
                .build();
    }

    @Override
    public List<CategoryInsightDto> getCategorySpending(Long userId, int days, Long accountId) {
        List<Transaction> txns = accountId != null
                ? transactionRepository.findAllByAccount(accountId)
                : transactionRepository.findAllByUserId(userId);
        return buildCategoryBreakdown(txns);
    }

    @Override
    public List<MonthlyInsightDto> getMonthlySpending(Long userId, int months) {
        List<Transaction> txns = transactionRepository.findAllByUserId(userId);
        return buildMonthlyTimeline(txns);
    }

    @Override
    public AnalyticsDTO getLatestAnalyticsByAccount(Long accountId) {
        List<Transaction> txns = transactionRepository.findAllByBankAccountId(accountId);

        AnalyticsDTO analytics = new AnalyticsDTO();
        analytics.setMonthlyTimeline(buildMonthlyTimelineData(txns));
        analytics.setBurnRate(buildBurnRateData(txns));
        analytics.setTopCategories(buildTopCategoriesData(txns));
        analytics.setTopMerchants(buildTopMerchantsData(txns));
        analytics.setSpendingTypes(buildSpendingTypesData(txns));
        analytics.setAverages(buildAveragesData(txns));
        analytics.setYearOverYear(buildYearOverYearData(txns));

        return analytics;
    }

    @Override
    public DashboardStatsDto getDashboardStatsByAccount(Long accountId) {
        List<Transaction> txns = transactionRepository.findAllByBankAccountId(accountId);
        double totalSpent = txns.stream().mapToDouble(t -> Math.abs(t.getAmount().doubleValue())).sum();

        return DashboardStatsDto.builder()
                .totalSpent(totalSpent)
                .totalIncome(0.0)
                .budgetLeft(0.0)
                .transactionCount((long) txns.size())
                .subscriptionCount(0L)
                .savingsRate(0.0)
                .build();
    }

    @Override
    public List<MonthlyInsightDto> getMonthlySpendingByAccount(Long accountId, int months) {
        List<Transaction> txns = transactionRepository.findAllByBankAccountId(accountId);
        return buildMonthlyTimeline(txns);
    }
}
