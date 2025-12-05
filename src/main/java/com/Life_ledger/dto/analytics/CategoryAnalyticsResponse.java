package com.Life_ledger.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryAnalyticsResponse {
    private String categoryName;
    private double total;
}

