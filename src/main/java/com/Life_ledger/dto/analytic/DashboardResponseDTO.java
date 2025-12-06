package com.Life_ledger.dto.analytic;

import lombok.*;
import java.util.List;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponseDTO {

    private List<MonthlyInsightDto> monthlyTimeline;
    private List<CategoryInsightDto> categories;
    private List<MerchantInsightDto> merchants;

    private RecurringVsOneTimeDto recurringVsOneTime;
    private BurnRateDto burnRate;

    private List<MonthlyInsightDto> yearOverYear;
}
