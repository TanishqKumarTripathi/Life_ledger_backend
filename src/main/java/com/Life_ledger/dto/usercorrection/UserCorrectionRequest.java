package com.Life_ledger.dto.usercorrection;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCorrectionRequest {
    private String correctedMerchant;
    private BigDecimal correctedAmount;
    private String notes;
}
