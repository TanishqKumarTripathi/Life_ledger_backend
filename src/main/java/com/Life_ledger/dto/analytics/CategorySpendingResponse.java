package com.Life_ledger.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpendingResponse {
    private String categoryName;
    private BigDecimal totalAmount;
}