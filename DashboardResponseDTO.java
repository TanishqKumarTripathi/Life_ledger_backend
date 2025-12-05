package com.Life_ledger.dto.frontendDTO;

import lombok.*;
import java.util.List;

import com.Life_ledger.dto.analytic.BurnRateDto;
import com.Life_ledger.dto.analytic.CategoryInsightDto;
import com.Life_ledger.dto.analytic.MerchantInsightDto;

@Data
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
