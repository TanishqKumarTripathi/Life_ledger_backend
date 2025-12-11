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

    private static final List<String> COLORS = List.of(
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444",
            "#8B5CF6", "#EC4899", "#06B6D4", "#F43F5E");

    // ------------- Public entry points (unchanged signatures) ----------------

    @Override
    public DashboardResponseDTO getDashboard(Long userId, Long accountId) {
        List<Transaction> txns = transactionRepository.findAllByBankAccountId(accountId);

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
    public DashboardStatsDto getDashboardStats(Long userId) {
        return new DashboardStatsDto();
    }

    @Override
    public DashboardStatsDto getDashboardStatsByAccount(Long accountId) {
        return new DashboardStatsDto();
    }

    @Override
    public List<CategoryInsightDto> getCategorySpending(Long userId, int days, Long accountId) {
        List<Transaction> txns = accountId != null
                ? transactionRepository.findAllByBankAccountId(accountId)
                : transactionRepository.findAllByUserId(userId);
        return buildCategoryBreakdown(txns);
    }

    @Override
    public List<MonthlyInsightDto> getMonthlySpending(Long userId, int months) {
        List<Transaction> txns = transactionRepository.findAllByUserId(userId);
        return buildMonthlyTimeline(txns);
    }

    @Override
    public List<MonthlyInsightDto> getMonthlySpendingByAccount(Long accountId, int months) {
        List<Transaction> txns = transactionRepository.findAllByBankAccountId(accountId);
        return buildMonthlyTimeline(txns);
    }

    // ---------------------- Core helpers & fixes -------------------------------

    /**
     * Decide whether a transaction is a debit (spend).
     *
     * IMPORTANT: This implementation assumes debits are stored as negative amounts.
     * If your model stores debits as positive values and credits as negative, invert this logic.
     */
    private boolean isDebit(Transaction t) {
        if (t == null || t.getAmount() == null) return false;
        return t.getAmount().doubleValue() < 0;
    }

    /**
     * Normalizes merchant strings so similar merchants group together.
     */
    private String cleanMerchantName(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";
        String s = raw.toUpperCase().trim();

        // remove common tokens
        s = s.replaceAll("UPI[-_ ]?", "");
        s = s.replaceAll("PAYTM|PTYS|PTYBL|PAYAXIS|HDFCBANK|YESB0PTM|YESB0YBL|UTIB|OKAXIS|ICICI|AXISBANK", "");
        s = s.replaceAll("[^A-Z0-9 @.&]", " "); // keep letters/digits/spaces/@
        s = s.replaceAll("\\s{2,}", " ").trim();

        // If it contains an obvious merchant name, try to extract it
        if (s.contains("ZOMATO")) return "Zomato";
        if (s.contains("IRCTC") || s.contains("RAIL")) return "IRCTC";
        if (s.contains("GOOGLE") || s.contains("PLAYSTORE") || s.contains("GPAY")) return "Google Play";
        if (s.contains("AMAZON")) return "Amazon";
        if (s.contains("BURGER") || s.contains("RESTAUR")) return "Restaurant";
        if (s.length() > 20) {
            // take first token if too long
            return s.split(" ")[0];
        }
        return s;
    }

    // ---------------------- Monthly timeline -------------------------------

    private List<MonthlyInsightDto> buildMonthlyTimeline(List<Transaction> txns) {
        // We'll produce last 12 months timeline but count only DEBITS as spending.
        LocalDate now = LocalDate.now();
        Map<YearMonth, Double> monthly = new LinkedHashMap<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            monthly.put(ym, 0.0);
        }

        txns.stream()
                .filter(this::isDebit)
                .forEach(t -> {
                    LocalDate d = t.getDate();
                    if (d == null) return;
                    YearMonth ym = YearMonth.from(d);
                    if (monthly.containsKey(ym)) {
                        monthly.put(ym, monthly.getOrDefault(ym, 0.0) + Math.abs(t.getAmount().doubleValue()));
                    }
                });

        return monthly.entrySet().stream()
                .map(e -> new MonthlyInsightDto(e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + e.getKey().getYear(), e.getValue()))
                .toList();
    }

    // ---------------------- Category breakdown -------------------------------

    private List<CategoryInsightDto> buildCategoryBreakdown(List<Transaction> txns) {
        Map<String, CategoryInsightDto> map = new HashMap<>();

        txns.stream()
                .filter(this::isDebit)
                .forEach(t -> {
                    String category = (t.getCategory() != null && t.getCategory().getName() != null)
                            ? t.getCategory().getName()
                            : "Uncategorized";

                    CategoryInsightDto dto = map.computeIfAbsent(category, k -> new CategoryInsightDto(k, 0, 0, null));
                    dto.setAmount(dto.getAmount() + Math.abs(t.getAmount().doubleValue()));
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

    private List<MerchantInsightDto> buildMerchantBreakdown(List<Transaction> txns) {
        Map<String, MerchantInsightDto> map = new HashMap<>();

        txns.stream()
                .filter(this::isDebit) // only count spends
                .forEach(t -> {
                    String rawMerchant = t.getMerchant();
                    String merchant = cleanMerchantName(rawMerchant);

                    map.putIfAbsent(merchant, new MerchantInsightDto(merchant, 0, 0));
                    MerchantInsightDto dto = map.get(merchant);
                    dto.setAmount(dto.getAmount() + Math.abs(t.getAmount().doubleValue()));
                    dto.setCount(dto.getCount() + 1);
                });

        return map.values().stream()
                .sorted(Comparator.comparingDouble(MerchantInsightDto::getAmount).reversed())
                .limit(8)
                .toList();
    }

    // ---------------------- Recurring vs One-time -------------------------------

    private RecurringVsOneTimeDto buildRecurringSplit(List<Transaction> txns) {
        // Group by normalized merchant (only DEBITS)
        Map<String, Long> merchantFreq = txns.stream()
                .filter(this::isDebit)
                .map(t -> cleanMerchantName(t.getMerchant()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        double recurringTotal = 0;
        double oneTimeTotal = 0;

        for (Transaction t : txns) {
            if (!isDebit(t)) continue;
            String merchant = cleanMerchantName(t.getMerchant());
            long count = merchantFreq.getOrDefault(merchant, 1L);
            double amt = Math.abs(t.getAmount().doubleValue());

            // treat merchant with >= 3 occurrences as recurring
            if (count >= 3) recurringTotal += amt;
            else oneTimeTotal += amt;
        }

        return new RecurringVsOneTimeDto(recurringTotal, oneTimeTotal);
    }

    // ---------------------- Burn rate (last 30 days) -------------------------------

    private BurnRateDto buildBurnRate(List<Transaction> txns) {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(29); // last 30 days inclusive

        List<Transaction> last30 = txns.stream()
                .filter(this::isDebit)
                .filter(t -> {
                    LocalDate d = t.getDate();
                    return d != null && (!d.isBefore(start) && !d.isAfter(now));
                })
                .toList();

        double totalLast30 = last30.stream()
                .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                .sum();

        double daily = totalLast30 / 30.0;
        double projected = daily * 30.0;

        // days left in current month (useful if you want calendar projected)
        int daysLeft = now.lengthOfMonth() - now.getDayOfMonth();

        return BurnRateDto.builder()
                .current(totalLast30)
                .daily(daily)
                .projected(projected)
                .daysLeft(daysLeft)
                .build();
    }

    // ---------------------- Analytics DTO builders (adapted) -------------------------------

    private List<AnalyticsDTO.MonthlyTimelineData> buildMonthlyTimelineData(List<Transaction> txns) {
        return buildMonthlyTimeline(txns).stream()
                .map(m -> {
                    AnalyticsDTO.MonthlyTimelineData data = new AnalyticsDTO.MonthlyTimelineData();
                    data.setMonth(m.getMonth());
                    data.setExpenses(m.getAmount());
                    data.setIncome(0.0);   // income extraction can be added similarly if needed
                    data.setSavings(0.0);
                    return data;
                })
                .toList();
    }

    private AnalyticsDTO.BurnRateData buildBurnRateData(List<Transaction> txns) {
        BurnRateDto burnRate = buildBurnRate(txns);
        AnalyticsDTO.BurnRateData data = new AnalyticsDTO.BurnRateData();
        data.setCurrentRate(burnRate.getDaily());
        data.setTrend("stable"); // you can compute trend over time if desired
        data.setDaysToZero((int) burnRate.getDaysLeft());
        return data;
    }

    private List<AnalyticsDTO.CategoryData> buildTopCategoriesData(List<Transaction> txns) {
        double total = txns.stream().filter(this::isDebit).mapToDouble(t -> Math.abs(t.getAmount().doubleValue())).sum();
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

    private List<AnalyticsDTO.MerchantData> buildTopMerchantsData(List<Transaction> txns) {
        double total = txns.stream().filter(this::isDebit).mapToDouble(t -> Math.abs(t.getAmount().doubleValue())).sum();
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
        // Compute totals and actual period length (in days) from transaction dates
        List<Transaction> debits = txns.stream().filter(this::isDebit).toList();

        if (debits.isEmpty()) {
            AnalyticsDTO.AveragesData data = new AnalyticsDTO.AveragesData();
            data.setDailySpending(0.0);
            data.setWeeklySpending(0.0);
            data.setMonthlySpending(0.0);
            return data;
        }

        LocalDate minDate = debits.stream().map(Transaction::getDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = debits.stream().map(Transaction::getDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(LocalDate.now());

        long days = ChronoUnit.DAYS.between(minDate, maxDate) + 1;
        if (days <= 0) days = 1;

        double totalAmount = debits.stream().mapToDouble(t -> Math.abs(t.getAmount().doubleValue())).sum();

        double daily = totalAmount / (double) days;
        double weekly = daily * 7.0;
        double monthly = daily * 30.0;

        AnalyticsDTO.AveragesData data = new AnalyticsDTO.AveragesData();
        data.setDailySpending(daily);
        data.setWeeklySpending(weekly);
        data.setMonthlySpending(monthly);
        return data;
    }

    private AnalyticsDTO.YearOverYearData buildYearOverYearData(List<Transaction> txns) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int previousYear = currentYear - 1;

        double currentYearTotal = txns.stream()
                .filter(this::isDebit)
                .filter(t -> t.getDate() != null && t.getDate().getYear() == currentYear)
                .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                .sum();

        double previousYearTotal = txns.stream()
                .filter(this::isDebit)
                .filter(t -> t.getDate() != null && t.getDate().getYear() == previousYear)
                .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
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
            data.setTrend("stable"); // no prior-year data to compare
        }

        return data;
    }
}
