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

        List<Transaction> txns = transactionRepository.findAllByAccount(accountId);

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
}
