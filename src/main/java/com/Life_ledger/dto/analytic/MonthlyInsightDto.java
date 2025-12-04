package com.Life_ledger.dto.analytic;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyInsightDto {
    private String month; // e.g., "Jan 2025"
    private double totalIncome;
    private double totalSpending;
}
