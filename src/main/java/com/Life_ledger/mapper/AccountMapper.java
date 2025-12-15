package com.Life_ledger.mapper;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.entity.BankAccount;

public class AccountMapper {

    public static BankAccount toEntity(AccountRequest request) {
        return BankAccount.builder()
                .bankName(request.getBankName())
                .accountName(request.getAccountName())
                .build();
    }

    public static AccountResponse toResponse(BankAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountName(account.getAccountName() != null ? account.getAccountName() : "HDFC Account")
                .bankName(account.getBankName() != null ? account.getBankName() : "HDFC Bank")
                .last4Digits(account.getLast4Digits())
                .build();
    }

    public static AccountResponse fromEntity(BankAccount account) {
        if (account == null)
            return null;
        return AccountResponse.builder()
                .id(account.getId())
                .bankName(account.getBankName() != null ? account.getBankName() : "HDFC Bank")
                .accountName(account.getAccountName() != null ? account.getAccountName() : "HDFC Account")
                .last4Digits(account.getLast4Digits())
                .build();
    }
}