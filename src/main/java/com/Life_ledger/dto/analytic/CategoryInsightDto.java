package com.Life_ledger.dto.analytic;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryInsightDto {
    private String categoryName;
    private double totalAmount;
}
