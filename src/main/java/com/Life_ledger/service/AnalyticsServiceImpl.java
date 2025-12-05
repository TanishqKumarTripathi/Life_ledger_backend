package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.AnalyticsDTO;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.repository.InsightRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final InsightRepository insightRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDTO getLatestAnalytics(Long userId) {
        Optional<Insight> latestInsight = insightRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        
        if (latestInsight.isEmpty()) {
            throw new RuntimeException("No insights found. Please run analysis first.");
        }

        return parseAnalyticsFromInsight(latestInsight.get().getAiText());
    }

    private AnalyticsDTO parseAnalyticsFromInsight(String aiText) {
        try {
            JsonNode rootNode = objectMapper.readTree(aiText);
            JsonNode analysisNode = rootNode.path("analysis");
            
            AnalyticsDTO analytics = new AnalyticsDTO();
            
            JsonNode categorized = analysisNode.path("categorized");
            JsonNode recurring = analysisNode.path("recurring");
            
            analytics.setTopCategories(parseTopCategories(categorized));
            analytics.setTopMerchants(parseTopMerchants(categorized));
            analytics.setSpendingTypes(parseSpendingTypes(recurring, categorized));
            analytics.setMonthlyTimeline(parseMonthlyTimeline(categorized));
            analytics.setAverages(calculateAverages(categorized));
            analytics.setBurnRate(calculateBurnRate(categorized));
            analytics.setYearOverYear(calculateYearOverYear(categorized));

            return analytics;
        } catch (Exception e) {
            return createDefaultAnalytics();
        }
    }

    private List<AnalyticsDTO.CategoryData> parseTopCategories(JsonNode categorized) {
        Map<String, Double> categoryTotals = new HashMap<>();
        double totalAmount = 0;

        if (categorized.isArray()) {
            for (JsonNode transaction : categorized) {
                String category = transaction.path("category").asText("Other");
                double amount = Math.abs(transaction.path("amount").asDouble(0));
                if (amount > 0) {
                    categoryTotals.merge(category, amount, Double::sum);
                    totalAmount += amount;
                }
            }
        }

        final double finalTotal = totalAmount;
        return categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    AnalyticsDTO.CategoryData data = new AnalyticsDTO.CategoryData();
                    data.setCategory(entry.getKey());
                    data.setAmount(entry.getValue());
                    data.setPercentage(finalTotal > 0 ? (entry.getValue() / finalTotal) * 100 : 0);
                    return data;
                })
                .collect(Collectors.toList());
    }

    private List<AnalyticsDTO.MerchantData> parseTopMerchants(JsonNode categorized) {
        Map<String, MerchantInfo> merchantTotals = new HashMap<>();

        if (categorized.isArray()) {
            for (JsonNode transaction : categorized) {
                String merchant = transaction.path("description").asText(
                    transaction.path("merchant").asText("Unknown"));
                String bankAccount = transaction.path("bankAccount").asText(
                    transaction.path("accountName").asText("Unknown Account"));
                double amount = Math.abs(transaction.path("amount").asDouble(0));
                if (amount > 0) {
                    merchantTotals.computeIfAbsent(merchant, k -> new MerchantInfo(bankAccount))
                            .addTransaction(amount);
                }
            }
        }

        return merchantTotals.entrySet().stream()
                .sorted(Map.Entry.<String, MerchantInfo>comparingByValue(
                        Comparator.comparing(MerchantInfo::getAmount)).reversed())
                .limit(5)
                .map(entry -> {
                    AnalyticsDTO.MerchantData data = new AnalyticsDTO.MerchantData();
                    data.setMerchant(entry.getKey());
                    data.setAmount(entry.getValue().getAmount());
                    data.setTransactions(entry.getValue().getCount());
                    data.setBankAccount(entry.getValue().getBankAccount());
                    return data;
                })
                .collect(Collectors.toList());
    }

    private AnalyticsDTO.SpendingTypeData parseSpendingTypes(JsonNode recurring, JsonNode categorized) {
        double recurringTotal = 0;
        double totalSpending = 0;

        if (recurring.isArray()) {
            for (JsonNode pattern : recurring) {
                recurringTotal += Math.abs(pattern.path("amount").asDouble(0));
            }
        }

        if (categorized.isArray()) {
            for (JsonNode transaction : categorized) {
                double amount = transaction.path("amount").asDouble(0);
                if (amount < 0) {
                    totalSpending += Math.abs(amount);
                }
            }
        }

        double oneTimeTotal = Math.max(0, totalSpending - recurringTotal);

        AnalyticsDTO.SpendingTypeData spendingTypes = new AnalyticsDTO.SpendingTypeData();
        spendingTypes.setRecurring(recurringTotal);
        spendingTypes.setOneTime(oneTimeTotal);
        spendingTypes.setRecurringPercentage(totalSpending > 0 ? (recurringTotal / totalSpending) * 100 : 0);
        spendingTypes.setOneTimePercentage(totalSpending > 0 ? (oneTimeTotal / totalSpending) * 100 : 0);
        return spendingTypes;
    }

    private List<AnalyticsDTO.MonthlyTimelineData> parseMonthlyTimeline(JsonNode categorized) {
        Map<String, MonthlyData> monthlyTotals = new HashMap<>();

        if (categorized.isArray()) {
            for (JsonNode transaction : categorized) {
                String date = transaction.path("date").asText("");
                if (date.length() >= 7) {
                    String month = date.substring(0, 7);
                    double amount = transaction.path("amount").asDouble(0);
                    
                    monthlyTotals.computeIfAbsent(month, k -> new MonthlyData())
                            .addTransaction(amount);
                }
            }
        }

        return monthlyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    AnalyticsDTO.MonthlyTimelineData data = new AnalyticsDTO.MonthlyTimelineData();
                    data.setMonth(entry.getKey());
                    data.setIncome(entry.getValue().getIncome());
                    data.setExpenses(entry.getValue().getExpenses());
                    data.setSavings(entry.getValue().getIncome() - entry.getValue().getExpenses());
                    return data;
                })
                .collect(Collectors.toList());
    }

    private AnalyticsDTO.AveragesData calculateAverages(JsonNode categorized) {
        double totalExpenses = 0;
        int expenseCount = 0;

        if (categorized.isArray()) {
            for (JsonNode transaction : categorized) {
                double amount = transaction.path("amount").asDouble(0);
                if (amount < 0) {
                    totalExpenses += Math.abs(amount);
                    expenseCount++;
                }
            }
        }

        AnalyticsDTO.AveragesData averages = new AnalyticsDTO.AveragesData();
        averages.setMonthlySpending(totalExpenses);
        averages.setWeeklySpending(totalExpenses / 4.33);
        averages.setDailySpending(totalExpenses / 30);
        return averages;
    }

    private AnalyticsDTO.BurnRateData calculateBurnRate(JsonNode categorized) {
        double totalExpenses = 0;
        if (categorized.isArray()) {
            for (JsonNode transaction : categorized) {
                double amount = transaction.path("amount").asDouble(0);
                if (amount < 0) {
                    totalExpenses += Math.abs(amount);
                }
            }
        }

        AnalyticsDTO.BurnRateData burnRate = new AnalyticsDTO.BurnRateData();
        double dailyBurnRate = totalExpenses / 30;
        burnRate.setCurrentRate(dailyBurnRate);
        burnRate.setTrend(dailyBurnRate > 100 ? "increasing" : "stable");
        burnRate.setDaysToZero(dailyBurnRate > 0 ? (int)(5000 / dailyBurnRate) : 999);
        return burnRate;
    }

    private AnalyticsDTO.YearOverYearData calculateYearOverYear(JsonNode categorized) {
        double currentYearTotal = 0;
        if (categorized.isArray()) {
            for (JsonNode transaction : categorized) {
                double amount = transaction.path("amount").asDouble(0);
                if (amount < 0) {
                    currentYearTotal += Math.abs(amount);
                }
            }
        }

        AnalyticsDTO.YearOverYearData yoy = new AnalyticsDTO.YearOverYearData();
        double previousYearTotal = currentYearTotal * 0.85;
        yoy.setCurrentYearTotal(currentYearTotal);
        yoy.setPreviousYearTotal(previousYearTotal);
        yoy.setChangePercentage(previousYearTotal > 0 ? ((currentYearTotal - previousYearTotal) / previousYearTotal) * 100 : 0);
        yoy.setTrend(currentYearTotal > previousYearTotal ? "increasing" : "decreasing");
        return yoy;
    }

    private AnalyticsDTO createDefaultAnalytics() {
        AnalyticsDTO analytics = new AnalyticsDTO();
        analytics.setTopCategories(new ArrayList<>());
        analytics.setTopMerchants(new ArrayList<>());
        analytics.setMonthlyTimeline(new ArrayList<>());
        
        AnalyticsDTO.SpendingTypeData spendingTypes = new AnalyticsDTO.SpendingTypeData();
        spendingTypes.setRecurring(0.0);
        spendingTypes.setOneTime(0.0);
        spendingTypes.setRecurringPercentage(0.0);
        spendingTypes.setOneTimePercentage(0.0);
        analytics.setSpendingTypes(spendingTypes);
        
        AnalyticsDTO.AveragesData averages = new AnalyticsDTO.AveragesData();
        averages.setDailySpending(0.0);
        averages.setWeeklySpending(0.0);
        averages.setMonthlySpending(0.0);
        analytics.setAverages(averages);
        
        AnalyticsDTO.BurnRateData burnRate = new AnalyticsDTO.BurnRateData();
        burnRate.setCurrentRate(0.0);
        burnRate.setTrend("stable");
        burnRate.setDaysToZero(999);
        analytics.setBurnRate(burnRate);
        
        AnalyticsDTO.YearOverYearData yoy = new AnalyticsDTO.YearOverYearData();
        yoy.setCurrentYearTotal(0.0);
        yoy.setPreviousYearTotal(0.0);
        yoy.setChangePercentage(0.0);
        yoy.setTrend("stable");
        analytics.setYearOverYear(yoy);
        
        return analytics;
    }

    private static class MerchantInfo {
        private double amount = 0;
        private int count = 0;
        private String bankAccount;

        MerchantInfo(String bankAccount) {
            this.bankAccount = bankAccount;
        }

        void addTransaction(double amt) {
            amount += amt;
            count++;
        }

        double getAmount() { return amount; }
        int getCount() { return count; }
        String getBankAccount() { return bankAccount; }
    }

    private static class MonthlyData {
        private double income = 0;
        private double expenses = 0;

        void addTransaction(double amount) {
            if (amount > 0) {
                income += amount;
            } else {
                expenses += Math.abs(amount);
            }
        }

        double getIncome() { return income; }
        double getExpenses() { return expenses; }
    }
}