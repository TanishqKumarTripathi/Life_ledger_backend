package com.Life_ledger.service;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.entity.BankAccount;

import java.util.List;

public interface AccountService {
    BankAccount createAccount(Long userId, AccountRequest request);

    List<AccountResponse> getUserAccounts(Long userId);

    BankAccount updateAccount(Long userId, AccountRequest request);

    String deleteAccount(Long userId, BankAccount account);
}
