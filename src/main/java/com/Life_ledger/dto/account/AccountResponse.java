package com.Life_ledger.dto.account;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private Long id;
    private String accountName;
    private String bankName;
    private String last4Digits; // only last 4 digits shown


}
