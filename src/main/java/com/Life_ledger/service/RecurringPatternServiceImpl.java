package com.Life_ledger.service.impl;

import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.RecurringPatternRepository;
import com.Life_ledger.service.RecurringPatternService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringPatternServiceImpl implements RecurringPatternService {

    private final RecurringPatternRepository recurringPatternRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public RecurringPattern createRecurringPattern(RecurringPattern recurringPattern) {
        return recurringPatternRepository.save(recurringPattern);
    }

    @Override
    public RecurringPattern updateRecurringPattern(Long id, RecurringPattern recurringPattern) {
        RecurringPattern existing = recurringPatternRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring Pattern not found"));

        existing.setMerchant(recurringPattern.getMerchant());
        existing.setAmount(recurringPattern.getAmount());
        existing.setFrequency(recurringPattern.getFrequency());
        existing.setNextDueDate(recurringPattern.getNextDueDate());
        existing.setBankAccount(recurringPattern.getBankAccount());

        return recurringPatternRepository.save(existing);
    }

    @Override
    public RecurringPattern getRecurringPattern(Long id) {
        return recurringPatternRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring Pattern not found"));
    }

    @Override
    public List<RecurringPattern> getAllRecurringPatterns() {
        return recurringPatternRepository.findAll();
    }

    @Override
    public void deleteRecurringPattern(Long id) {
        recurringPatternRepository.deleteById(id);
    }
}
