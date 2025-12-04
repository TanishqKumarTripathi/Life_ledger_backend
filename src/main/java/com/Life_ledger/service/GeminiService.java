package com.Life_ledger.service;

import com.Life_ledger.service.GeminiServiceImpl.Step;
import com.Life_ledger.service.GeminiServiceImpl.TaskStatus;

import java.util.Map;

public interface GeminiService {

    // simple test endpoints
    String testModel();

    String listModels();

    // ---- master async pipeline ----
    void analyzeUserTransactionsAsync(Long userId);

    void analyzeUserTransactionsAsync(Long userId, Long accountId);

    // ---- per-step manual triggers (optional, but useful for testing) ----
    void processCategorization(Long userId);

    void processCategorization(Long userId, Long accountId);

    void processRecurringAsync(Long userId);

    void processRecurringAsync(Long userId, Long accountId);

    void processAnomaliesAsync(Long userId);

    void processAnomaliesAsync(Long userId, Long accountId);

    void processSummaryAsync(Long userId);

    void processSummaryAsync(Long userId, Long accountId);

    // ---- status ----
    Map<Step, TaskStatus> getStatusForUserAccount(Long userId, Long accountId);

    TaskStatus getStepStatusForUserAccount(Long userId, Long accountId, Step step);
}
