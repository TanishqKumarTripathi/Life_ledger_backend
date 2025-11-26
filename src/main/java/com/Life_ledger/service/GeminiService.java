package com.Life_ledger.service;

public interface GeminiService {
    String testModel();

    String listModels();

    Object analyzeUserTransactions(Long userId);

}
