package com.Life_ledger.service;

import com.Life_ledger.entity.Insight;
import com.Life_ledger.entity.RecurringPattern;
import java.util.List;

public interface RecurringPatternService {

    RecurringPattern createRecurringPattern(RecurringPattern recurringPattern);

    RecurringPattern updateRecurringPattern(Long id, RecurringPattern recurringPattern);

    RecurringPattern getRecurringPattern(Long id);

    List<RecurringPattern> getAllRecurringPatterns();

    List<RecurringPattern> getRecurringPatternsByUserId(Long userId);

    void deleteRecurringPattern(Long id);

    void processInsightPatterns(Insight insight);
}