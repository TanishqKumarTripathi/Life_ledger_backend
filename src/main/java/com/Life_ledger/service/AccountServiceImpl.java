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
    private final EncryptionUtil encryptionUtil; // custom utility to encrypt/decrypt

    @Override
    public BankAccount createAccount(Long userId, AccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BankAccount account = AccountMapper.toEntity(request);
        account.setUser(user);

        // Encrypt full account number and store last 4 digits
        String encryptedNumber = encryptionUtil.encrypt(request.getAccountNumber());
        account.setEncryptedAccountNumber(encryptedNumber);
        account.setLast4Digits(request.getAccountNumber()
                .substring(request.getAccountNumber().length() - 4));

        BankAccount saved = bankAccountRepository.save(account);
        // return AccountMapper.toResponse(saved);
        return saved;
    }

    @Override
    public List<AccountResponse> getUserAccounts(Long userId) {
        List<BankAccount> accounts = bankAccountRepository.findByUserId(userId);
        return accounts.stream()
                .map(AccountMapper::toResponse)
                .collect(Collectors.toList());
    }
}
