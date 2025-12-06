package com.Life_ledger.dto.analytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DashboardStatsDto {
    private double totalSpent;
    private double totalIncome;
    private double budgetLeft;
    private long transactionCount;
    private long subscriptionCount;
    private double savingsRate;
}
