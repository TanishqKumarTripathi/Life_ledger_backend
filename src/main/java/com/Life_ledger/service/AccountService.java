package com.Life_ledger.service;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.dto.account.BankAccountSummaryDto;
import com.Life_ledger.entity.BankAccount;

import java.util.List;

public interface AccountService {
    BankAccount createAccount(Long userId, AccountRequest request);

    List<AccountResponse> getUserAccounts(Long userId);

    BankAccount updateAccount(Long userId, Long accountId, AccountRequest request);

    String deleteAccount(Long userId, Long accountId);

    List<BankAccountSummaryDto> getUserBankAccounts(Long userId);

    void deleteUserAccount(Long userId);

}
