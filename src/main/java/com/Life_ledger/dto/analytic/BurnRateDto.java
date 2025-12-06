package com.Life_ledger.dto.analytic;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BurnRateDto {
    private double current;
    private double daily;
    private double projected;
    private int daysLeft;
}
