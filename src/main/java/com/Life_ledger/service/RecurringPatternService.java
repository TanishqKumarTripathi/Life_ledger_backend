package com.Life_ledger.service;

import com.Life_ledger.entity.RecurringPattern;
import java.util.List;

public interface RecurringPatternService {

    RecurringPattern createRecurringPattern(RecurringPattern recurringPattern, Long userId);

    RecurringPattern updateRecurringPattern(Long id, RecurringPattern recurringPattern, Long userId);

    RecurringPattern getRecurringPattern(Long id, Long userId);

    List<RecurringPattern> getByBankAccount(Long bankAccountId, Long userId);

    void deleteRecurringPattern(Long id, Long userId);
}
