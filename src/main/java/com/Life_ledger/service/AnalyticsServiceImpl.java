package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.*;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final TransactionNormalizationService normalizationService;

    private static final List<String> COLORS = List.of(
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444",
            "#8B5CF6", "#EC4899", "#06B6D4", "#F43F5E");

    // ------------- Public entry points (unchanged signatures) ----------------

    @Override
    public DashboardResponseDTO getDashboard(Long userId, Long accountId) {
        List<Transaction> rawTxns = transactionRepository.findAllByBankAccountId(accountId);
        List<NormalizedTransaction> txns = normalizationService.normalizeTransactions(rawTxns);

        return DashboardResponseDTO.builder()
                .monthlyTimeline(buildMonthlyTimeline(txns))
                .categories(buildCategoryBreakdown(txns))
                .merchants(buildMerchantBreakdown(txns))
                .recurringVsOneTime(buildRecurringSplit(txns))
                .burnRate(buildBurnRate(txns))
                .yearOverYear(buildYearOverYearData(txns))
                .build();
    }

    @Override
    public AnalyticsDTO getLatestAnalytics(Long userId) {
        List<Transaction> rawTxns = transactionRepository.findAllByUserId(userId);
        List<NormalizedTransaction> txns = normalizationService.normalizeTransactions(rawTxns);

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
    public AnalyticsDTO getLatestAnalyticsByAccount(Long accountId) {
        List<Transaction> rawTxns = transactionRepository.findAllByBankAccountId(accountId);
        List<NormalizedTransaction> txns = normalizationService.normalizeTransactions(rawTxns);

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
    public DashboardStatsDto getDashboardStats(Long userId) {
        return new DashboardStatsDto();
    }

    @Override
    public DashboardStatsDto getDashboardStatsByAccount(Long accountId) {
        return new DashboardStatsDto();
    }

    @Override
    public List<CategoryInsightDto> getCategorySpending(Long userId, int days, Long accountId) {
        List<Transaction> rawTxns = accountId != null
                ? transactionRepository.findAllByBankAccountId(accountId)
                : transactionRepository.findAllByUserId(userId);
        List<NormalizedTransaction> txns = normalizationService.normalizeTransactions(rawTxns);
        return buildCategoryBreakdown(txns);
    }

    @Override
    public List<MonthlyInsightDto> getMonthlySpending(Long userId, int months) {
        List<Transaction> rawTxns = transactionRepository.findAllByUserId(userId);
        List<NormalizedTransaction> txns = normalizationService.normalizeTransactions(rawTxns);
        return buildMonthlyTimeline(txns);
    }

    @Override
    public List<MonthlyInsightDto> getMonthlySpendingByAccount(Long accountId, int months) {
        List<Transaction> rawTxns = transactionRepository.findAllByBankAccountId(accountId);
        List<NormalizedTransaction> txns = normalizationService.normalizeTransactions(rawTxns);
        return buildMonthlyTimeline(txns);
    }

    // ---------------------- Core helpers -------------------------------

    // ---------------------- Monthly timeline -------------------------------

    private List<MonthlyInsightDto> buildMonthlyTimeline(List<NormalizedTransaction> txns) {
        LocalDate now = LocalDate.now();
        Map<YearMonth, Double> monthly = new LinkedHashMap<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            monthly.put(ym, 0.0);
        }

        txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .forEach(t -> {
                    LocalDate d = t.getDate();
                    if (d == null) return;
                    YearMonth ym = YearMonth.from(d);
                    if (monthly.containsKey(ym)) {
                        monthly.put(ym, monthly.getOrDefault(ym, 0.0) + t.getSignedAmount().abs().doubleValue());
                    }
                });

        return monthly.entrySet().stream()
                .map(e -> new MonthlyInsightDto(e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + e.getKey().getYear(), e.getValue()))
                .toList();
    }

    // ---------------------- Category breakdown -------------------------------

    private List<CategoryInsightDto> buildCategoryBreakdown(List<NormalizedTransaction> txns) {
        Map<String, CategoryInsightDto> map = new HashMap<>();

        txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .forEach(t -> {
                    String category = t.getCategory() != null ? t.getCategory() : "Uncategorized";

                    CategoryInsightDto dto = map.computeIfAbsent(category, k -> new CategoryInsightDto(k, 0, 0, null));
                    dto.setAmount(dto.getAmount() + t.getSignedAmount().abs().doubleValue());
                    dto.setCount(dto.getCount() + 1);
                });

        List<CategoryInsightDto> sorted = map.values().stream()
                .sorted(Comparator.comparingDouble(CategoryInsightDto::getAmount).reversed())
                .limit(6)
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setColor(COLORS.get(i % COLORS.size()));
        }

        return sorted;
    }

    // ---------------------- Merchant breakdown -------------------------------

    private List<MerchantInsightDto> buildMerchantBreakdown(List<NormalizedTransaction> txns) {
        Map<String, MerchantInsightDto> map = new HashMap<>();

        txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .forEach(t -> {
                    String merchant = t.getCleanMerchant();

                    map.putIfAbsent(merchant, new MerchantInsightDto(merchant, 0, 0));
                    MerchantInsightDto dto = map.get(merchant);
                    dto.setAmount(dto.getAmount() + t.getSignedAmount().abs().doubleValue());
                    dto.setCount(dto.getCount() + 1);
                });

        return map.values().stream()
                .sorted(Comparator.comparingDouble(MerchantInsightDto::getAmount).reversed())
                .limit(8)
                .toList();
    }

    // ---------------------- Recurring vs One-time -------------------------------

    private RecurringVsOneTimeDto buildRecurringSplit(List<NormalizedTransaction> txns) {
        double recurringTotal = txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .filter(NormalizedTransaction::isRecurring)
                .mapToDouble(t -> t.getSignedAmount().abs().doubleValue())
                .sum();

        double oneTimeTotal = txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .filter(t -> !t.isRecurring())
                .mapToDouble(t -> t.getSignedAmount().abs().doubleValue())
                .sum();

        return new RecurringVsOneTimeDto(recurringTotal, oneTimeTotal);
    }

    // ---------------------- Burn rate (last 30 days) -------------------------------

    private BurnRateDto buildBurnRate(List<NormalizedTransaction> txns) {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(29);

        double totalLast30 = txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .filter(t -> {
                    LocalDate d = t.getDate();
                    return d != null && (!d.isBefore(start) && !d.isAfter(now));
                })
                .mapToDouble(t -> t.getSignedAmount().abs().doubleValue())
                .sum();

        double daily = totalLast30 / 30.0;
        double projected = daily * 30.0;
        int daysLeft = now.lengthOfMonth() - now.getDayOfMonth();

        return BurnRateDto.builder()
                .current(totalLast30)
                .daily(daily)
                .projected(projected)
                .daysLeft(daysLeft)
                .build();
    }

    // ---------------------- Analytics DTO builders (adapted) -------------------------------

    private List<AnalyticsDTO.MonthlyTimelineData> buildMonthlyTimelineData(List<NormalizedTransaction> txns) {
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

    private AnalyticsDTO.BurnRateData buildBurnRateData(List<NormalizedTransaction> txns) {
        BurnRateDto burnRate = buildBurnRate(txns);
        AnalyticsDTO.BurnRateData data = new AnalyticsDTO.BurnRateData();
        data.setCurrentRate(burnRate.getDaily());
        data.setTrend("stable");
        data.setDaysToZero((int) burnRate.getDaysLeft());
        return data;
    }

    private List<AnalyticsDTO.CategoryData> buildTopCategoriesData(List<NormalizedTransaction> txns) {
        double total = txns.stream().filter(NormalizedTransaction::isExpense).mapToDouble(t -> t.getSignedAmount().abs().doubleValue()).sum();
        return buildCategoryBreakdown(txns).stream()
                .map(c -> {
                    AnalyticsDTO.CategoryData data = new AnalyticsDTO.CategoryData();
                    data.setCategory(c.getName());
                    data.setAmount(c.getAmount());
                    data.setPercentage(total > 0 ? (c.getAmount() / total) * 100.0 : 0.0);
                    return data;
                })
                .toList();
    }

    private List<AnalyticsDTO.MerchantData> buildTopMerchantsData(List<NormalizedTransaction> txns) {
        double total = txns.stream().filter(NormalizedTransaction::isExpense).mapToDouble(t -> t.getSignedAmount().abs().doubleValue()).sum();
        return buildMerchantBreakdown(txns).stream()
                .map(m -> {
                    AnalyticsDTO.MerchantData data = new AnalyticsDTO.MerchantData();
                    data.setMerchant(m.getName());
                    data.setAmount(m.getAmount());
                    data.setTransactions((int) m.getCount());
                    data.setBankAccount("");
                    data.setPercentage(total > 0 ? (m.getAmount() / total) * 100.0 : 0.0);
                    return data;
                })
                .toList();
    }

    private AnalyticsDTO.SpendingTypeData buildSpendingTypesData(List<NormalizedTransaction> txns) {
        RecurringVsOneTimeDto split = buildRecurringSplit(txns);
        AnalyticsDTO.SpendingTypeData data = new AnalyticsDTO.SpendingTypeData();
        data.setRecurring(split.getRecurring());
        data.setOneTime(split.getOneTime());
        double total = split.getRecurring() + split.getOneTime();
        data.setRecurringPercentage(total > 0 ? (split.getRecurring() / total) * 100 : 0);
        data.setOneTimePercentage(total > 0 ? (split.getOneTime() / total) * 100 : 0);
        return data;
    }

    private AnalyticsDTO.AveragesData buildAveragesData(List<NormalizedTransaction> txns) {
        List<NormalizedTransaction> expenses = txns.stream().filter(NormalizedTransaction::isExpense).toList();

        if (expenses.isEmpty()) {
            AnalyticsDTO.AveragesData data = new AnalyticsDTO.AveragesData();
            data.setDailySpending(0.0);
            data.setWeeklySpending(0.0);
            data.setMonthlySpending(0.0);
            return data;
        }

        LocalDate minDate = expenses.stream().map(NormalizedTransaction::getDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = expenses.stream().map(NormalizedTransaction::getDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(LocalDate.now());

        long days = ChronoUnit.DAYS.between(minDate, maxDate) + 1;
        if (days <= 0) days = 1;

        double totalAmount = expenses.stream().mapToDouble(t -> t.getSignedAmount().abs().doubleValue()).sum();

        double daily = totalAmount / (double) days;
        double weekly = daily * 7.0;
        double monthly = daily * 30.0;

        AnalyticsDTO.AveragesData data = new AnalyticsDTO.AveragesData();
        data.setDailySpending(daily);
        data.setWeeklySpending(weekly);
        data.setMonthlySpending(monthly);
        return data;
    }

    private AnalyticsDTO.YearOverYearData buildYearOverYearData(List<NormalizedTransaction> txns) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int previousYear = currentYear - 1;

        double currentYearTotal = txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .filter(t -> t.getDate() != null && t.getDate().getYear() == currentYear)
                .mapToDouble(t -> t.getSignedAmount().abs().doubleValue())
                .sum();

        double previousYearTotal = txns.stream()
                .filter(NormalizedTransaction::isExpense)
                .filter(t -> t.getDate() != null && t.getDate().getYear() == previousYear)
                .mapToDouble(t -> t.getSignedAmount().abs().doubleValue())
                .sum();

        AnalyticsDTO.YearOverYearData data = new AnalyticsDTO.YearOverYearData();
        data.setCurrentYearTotal(currentYearTotal);
        data.setPreviousYearTotal(previousYearTotal);

        if (previousYearTotal > 0.0) {
            double change = ((currentYearTotal - previousYearTotal) / previousYearTotal) * 100.0;
            data.setChangePercentage(change);
            data.setTrend(change > 0 ? "increasing" : "decreasing");
        } else {
            data.setChangePercentage(0.0);
            data.setTrend("stable");
        }

        return data;
    }
}
