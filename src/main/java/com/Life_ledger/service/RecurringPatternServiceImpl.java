package com.Life_ledger.service;

import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.RecurringPatternRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringPatternServiceImpl implements RecurringPatternService {

    private final RecurringPatternRepository recurringPatternRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public RecurringPattern createRecurringPattern(RecurringPattern recurringPattern, Long userId) {
        BankAccount account = bankAccountRepository.findById(recurringPattern.getBankAccount().getId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized bank account access");
        }

        recurringPattern.setBankAccount(account);
        return recurringPatternRepository.save(recurringPattern);
    }

    @Override
    public RecurringPattern updateRecurringPattern(Long id, RecurringPattern updated, Long userId) {
        RecurringPattern existing = getRecurringPattern(id, userId);

        existing.setMerchant(updated.getMerchant());
        existing.setAmount(updated.getAmount());
        existing.setFrequency(updated.getFrequency());
        existing.setReason(updated.getReason());
        existing.setNextDueDate(updated.getNextDueDate());

        return recurringPatternRepository.save(existing);
    }

    @Override
    public RecurringPattern getRecurringPattern(Long id, Long userId) {
        RecurringPattern pattern = recurringPatternRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring Pattern not found"));

        if (!pattern.getBankAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return pattern;
    }

    @Override
    public List<RecurringPattern> getByBankAccount(Long bankAccountId, Long userId) {
        BankAccount account = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return recurringPatternRepository.findByBankAccount_Id(bankAccountId);
    }

    @Override
    public void deleteRecurringPattern(Long id, Long userId) {
        RecurringPattern pattern = getRecurringPattern(id, userId);
        recurringPatternRepository.delete(pattern);
    }
}
