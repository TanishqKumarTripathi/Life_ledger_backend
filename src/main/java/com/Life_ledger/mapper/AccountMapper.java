package com.Life_ledger.mapper;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.entity.BankAccount;

public class AccountMapper {

    public static BankAccount toEntity(AccountRequest request) {
        return BankAccount.builder()
                .accountName(request.getAccountName())
                .bankName(request.getBankName())
                .build();
    }

    public static AccountResponse toResponse(BankAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .bankName(account.getBankName())
                .last4Digits(account.getLast4Digits())
                .build();
    }

    public static AccountResponse fromEntity(BankAccount account) {
        if (account == null)
            return null;
        return AccountResponse.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .bankName(account.getBankName())
                .last4Digits(account.getLast4Digits())
                .build();
    }
}
