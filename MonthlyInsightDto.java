package com.Life_ledger.dto.frontendDTO;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyInsightDto {
    private String month;
    private double amount;
}
