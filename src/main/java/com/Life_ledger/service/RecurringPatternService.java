package com.Life_ledger.service;

import com.Life_ledger.entity.RecurringPattern;
import java.util.List;

public interface RecurringPatternService {

    RecurringPattern createRecurringPattern(RecurringPattern recurringPattern);

    RecurringPattern updateRecurringPattern(Long id, RecurringPattern recurringPattern);

    RecurringPattern getRecurringPattern(Long id);

    List<RecurringPattern> getAllRecurringPatterns();

    void deleteRecurringPattern(Long id);
}
