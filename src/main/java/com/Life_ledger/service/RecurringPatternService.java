package com.Life_ledger.service;

import com.Life_ledger.entity.Insight;
import com.Life_ledger.entity.RecurringPattern;
import java.util.List;

public interface RecurringPatternService {

    RecurringPattern createRecurringPattern(RecurringPattern recurringPattern, Long userId);
    
    // Backward compatibility method
    default RecurringPattern createRecurringPattern(RecurringPattern recurringPattern) {
        return createRecurringPattern(recurringPattern, null);
    }

    RecurringPattern updateRecurringPattern(Long id, RecurringPattern recurringPattern);

    RecurringPattern getRecurringPattern(Long id);

    List<RecurringPattern> getAllRecurringPatterns();

    List<RecurringPattern> getRecurringPatternsByUserId(Long userId);
    
    List<RecurringPattern> getRecurringPatternsByAccountId(Long accountId);

    void deleteRecurringPattern(Long id);

    void processInsightPatterns(Insight insight);
}