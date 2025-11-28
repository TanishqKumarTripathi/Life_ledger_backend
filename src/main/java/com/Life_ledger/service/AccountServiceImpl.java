package com.Life_ledger.service;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.AccountMapper;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

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
    public String deleteAccount(Long userId, Long accountId) {

        BankAccount existing = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!existing.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized deletion");
        }

        bankAccountRepository.delete(existing);
        return "Account deleted";
    }
}