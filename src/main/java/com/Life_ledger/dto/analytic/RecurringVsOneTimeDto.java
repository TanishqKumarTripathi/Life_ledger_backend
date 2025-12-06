package com.Life_ledger.dto.analytic;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecurringVsOneTimeDto {
    private double recurring;
    private double oneTime;
}
