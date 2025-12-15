package com.Life_ledger.dto.account;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BankAccountSummaryDto {
    private Long id;
    private String bankName;
    private String last4Digits;
}
