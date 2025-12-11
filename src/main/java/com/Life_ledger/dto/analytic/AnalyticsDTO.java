package com.Life_ledger.dto.analytic;

import lombok.Data;
import java.util.List;

@Data
public class AnalyticsDTO {
    private List<MonthlyTimelineData> monthlyTimeline;
    private BurnRateData burnRate;
    private List<CategoryData> topCategories;
    private List<MerchantData> topMerchants;
    private SpendingTypeData spendingTypes;
    private AveragesData averages;
    private YearOverYearData yearOverYear;

    @Data
    public static class MonthlyTimelineData {
        private String month;
        private Double income;
        private Double expenses;
        private Double savings;
    }

    @Data
    public static class BurnRateData {
        private Double currentRate;
        private String trend;
        private Integer daysToZero;
    }

    @Data
    public static class CategoryData {
        private String category;
        private Double amount;
        private Double percentage;
    }

    @Data
    public static class MerchantData {
        private String merchant;
        private Double amount;
        private Integer transactions;
        private String bankAccount;
    }

    @Data
    public static class SpendingTypeData {
        private Double recurring;
        private Double oneTime;
        private Double recurringPercentage;
        private Double oneTimePercentage;
    }

    @Data
    public static class AveragesData {
        private Double dailySpending;
        private Double weeklySpending;
        private Double monthlySpending;
    }

    @Data
    public static class YearOverYearData {
        private Double currentYearTotal;
        private Double previousYearTotal;
        private Double changePercentage;
        private String trend;
    }
}