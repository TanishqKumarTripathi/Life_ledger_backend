package com.Life_ledger.service;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.dto.account.BankAccountSummaryDto;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.AccountMapper;
import com.Life_ledger.repository.*;

import com.Life_ledger.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final FileImportRepository fileImportRepository;
    private final GoalRepository goalRepository;
    private final RecurringPatternRepository recurringPatternRepository;
    private final AnomalyRecordRepository anomalyRecordRepository;
    private final InsightRepository insightRepository;
    private final EncryptionUtil encryptionUtil;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Override
    public BankAccount createAccount(Long userId, AccountRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String encryptedNumber = encryptionUtil.encrypt(request.getAccountNumber());

        // Check for duplicate account
        if (bankAccountRepository.findFirstByEncryptedAccountNumberAndUserId(encryptedNumber, userId).isPresent()) {
            throw new RuntimeException("Account already exists for this user");
        }

        BankAccount account = AccountMapper.toEntity(request);
        account.setUser(user);
        account.setAccountName(request.getAccountName());
        account.setEncryptedAccountNumber(encryptedNumber);
        account.setLast4Digits(request.getAccountNumber()
                .substring(request.getAccountNumber().length() - 4));

        return bankAccountRepository.save(account);
    }

    @Override
    public List<AccountResponse> getUserAccounts(Long userId) {
        return bankAccountRepository.findByUserId(userId)
                .stream()
                .map(AccountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BankAccount updateAccount(Long userId, Long accountId, AccountRequest request) {

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized update");
        }

        account.setBankName(request.getBankName());

        // update encryption only if number changed
        if (request.getAccountNumber() != null) {
            String encrypted = encryptionUtil.encrypt(request.getAccountNumber());
            account.setEncryptedAccountNumber(encrypted);

            account.setLast4Digits(
                    request.getAccountNumber()
                            .substring(request.getAccountNumber().length() - 4));
        }

        return bankAccountRepository.save(account);
    }

    @Override
    @Transactional
    public String deleteAccount(Long userId, Long accountId) {

        BankAccount existing = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!existing.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized deletion");
        }

        // Delete all related entities first to avoid foreign key constraint violations
        // Order matters: delete child entities before parent
        
        // 1. Delete recurring patterns
        recurringPatternRepository.deleteByBankAccount_Id(accountId);
        
        // 2. Delete anomaly records
        anomalyRecordRepository.deleteByBankAccount_Id(accountId);

        // 3. Delete goals
        goalRepository.deleteByBankAccount_Id(accountId);
        
        // 4. Delete insights
        insightRepository.deleteByBankAccount_Id(accountId);
        
        // 5. Transactions will be deleted automatically due to cascade = CascadeType.ALL
        
        // 6. Now safe to delete the account
        bankAccountRepository.delete(existing);
        return "Account deleted";
    }

    @Override
    @Transactional
    public void deleteUserAccount(Long userId) {

        // 1️⃣ Collect bank account IDs first
        List<Long> accountIds = bankAccountRepository.findIdsByUserId(userId);

        if (!accountIds.isEmpty()) {

            // 2️⃣ Delete ALL data that depends on bank accounts
            anomalyRecordRepository.deleteByBankAccountIds(accountIds);
            transactionRepository.deleteByBankAccountIds(accountIds);
            recurringPatternRepository.deleteByBankAccountIds(accountIds);
            insightRepository.deleteByBankAccountIds(accountIds);

            // 3️⃣ Delete bank accounts themselves
            bankAccountRepository.deleteByIdIn(accountIds);
        }

        // 4️⃣ Delete user-rooted data (independent of accounts)
        subCategoryRepository.deleteByUserId(userId);
        categoryRepository.deleteByUserId(userId);
        fileImportRepository.deleteByUserId(userId);
        goalRepository.deleteByUserId(userId);

        // 5️⃣ Delete user LAST
        userRepository.deleteById(userId);
    }




    @Override
    public List<BankAccountSummaryDto> getUserBankAccounts(Long userId) {
        return bankAccountRepository.findByUserId(userId)
                .stream()
                .map(acc -> new BankAccountSummaryDto(
                        acc.getId(),
                        acc.getBankName(),
                        acc.getLast4Digits()))
                .collect(Collectors.toList());
    }

}