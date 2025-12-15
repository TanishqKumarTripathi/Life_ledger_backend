package com.Life_ledger.dto.analytic;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantInsightDto {
    private String name;
    private double amount;
    private long count;
}
