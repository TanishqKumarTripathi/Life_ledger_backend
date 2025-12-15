package com.Life_ledger.dto.analytic;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryInsightDto {
    private String name;
    private double amount;
    private long count;
    private String color;
}
