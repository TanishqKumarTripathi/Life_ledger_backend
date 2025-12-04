//package com.Life_ledger.service;
//
//import com.Life_ledger.Enum.TransactionEnum;
//import com.Life_ledger.dto.analytic.CategoryInsightDto;
//import com.Life_ledger.dto.analytic.DashboardStatsDto;
//import com.Life_ledger.dto.analytic.MonthlyInsightDto;
//import com.Life_ledger.entity.Transaction;
//import com.Life_ledger.repository.TransactionRepository;
//import com.Life_ledger.service.AnalyticsService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.format.TextStyle;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class AnalyticsServiceImpl implements AnalyticsService {
//
//    private final TransactionRepository transactionRepository;
//
//    private boolean isDebit(Transaction t) {
//        return t.getTypeTransaction() == TransactionEnum.DEBIT;
//    }
//
//    private boolean isCredit(Transaction t) {
//        return t.getTypeTransaction() == TransactionEnum.CREDIT;
//    }
//
//    // -------------------------------------------------------------
//    // 1️⃣ DASHBOARD SUMMARY
//    // -------------------------------------------------------------
//    @Override
//    public DashboardStatsDto getDashboardStats(Long userId) {
//
//        List<Transaction> txns = transactionRepository.findByBankAccount_User_Id(userId);
//
//        double totalSpent = txns.stream()
//                .filter(this::isDebit)
//                .mapToDouble(t -> t.getAmount().abs().doubleValue())
//                .sum();
//
//        double totalIncome = txns.stream()
//                .filter(this::isCredit)
//                .mapToDouble(t -> t.getAmount().doubleValue())
//                .sum();
//
//        double budgetLeft = totalIncome - totalSpent;
//
//        long subscriptions = txns.stream()
//                .map(Transaction::getMerchant)
//                .filter(Objects::nonNull)
//                .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
//                .values()
//                .stream()
//                .filter(count -> count > 1)
//                .count();
//
//        return new DashboardStatsDto(
//                totalSpent,
//                totalIncome,
//                budgetLeft,
//                txns.size(),
//                subscriptions,
//                totalIncome == 0 ? 0 : (budgetLeft / totalIncome));
//    }
//
//    // -------------------------------------------------------------
//    // 2️⃣ CATEGORY SPENDING (WITH ACCOUNT FILTER)
//    // -------------------------------------------------------------
//    @Override
//    public List<CategoryInsightDto> getCategorySpending(Long userId, int days, Long accountId) {
//        LocalDate cutoff = LocalDate.now().minusDays(days);
//
//        List<Transaction> txns;
//
//        if (accountId != null) {
//            txns = transactionRepository.findByBankAccount_User_IdAndBankAccount_IdAndDateAfter(
//                    userId, accountId, cutoff);
//        } else {
//            txns = transactionRepository.findByBankAccount_User_IdAndDateAfter(
//                    userId, cutoff);
//        }
//
//        Map<String, Double> categoryTotals = new HashMap<>();
//
//        for (Transaction t : txns) {
//            if (isDebit(t)) {
//                String category = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
//                categoryTotals.put(category,
//                        categoryTotals.getOrDefault(category, 0.0) + Math.abs(t.getAmount().doubleValue()));
//            }
//        }
//
//        return categoryTotals.entrySet().stream()
//                .map(e -> new CategoryInsightDto(e.getKey(), e.getValue()))
//                .collect(Collectors.toList());
//    }
//
//    // Keep original method for compatibility
//    @Override
//    public List<CategoryInsightDto> getCategorySpending(Long userId, int days) {
//        return getCategorySpending(userId, days, null);
//    }
//
//    // -------------------------------------------------------------
//    // 3️⃣ MONTHLY SPENDING
//    // -------------------------------------------------------------
//    @Override
//    public List<MonthlyInsightDto> getMonthlySpending(Long userId, int months) {
//
//        LocalDate start = LocalDate.now().minusMonths(months);
//
//        List<Transaction> txns = transactionRepository.findByBankAccount_User_IdAndDateAfter(userId, start);
//
//        Map<String, double[]> monthTotals = new TreeMap<>();
//
//        for (Transaction t : txns) {
//
//            String key = t.getDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
//                    + " " + t.getDate().getYear();
//
//            monthTotals.putIfAbsent(key, new double[] { 0, 0 }); // [income, spending]
//
//            if (isDebit(t)) {
//                monthTotals.get(key)[1] += Math.abs(t.getAmount().doubleValue());
//            } else if (isCredit(t)) {
//                monthTotals.get(key)[0] += t.getAmount().doubleValue();
//            }
//        }
//
//        return monthTotals.entrySet().stream()
//                .map(e -> new MonthlyInsightDto(e.getKey(), e.getValue()[0], e.getValue()[1]))
//                .collect(Collectors.toList());
//    }
//}
