package com.Life_ledger.service;

import com.Life_ledger.dto.transaction.*;
import com.Life_ledger.dto.usercorrection.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(Long userId, TransactionRequest request);

    TransactionResponse getTransaction(Long userId, Long id);

    List<TransactionResponse> getAllTransactions(Long userId, Long bankAccountId, Long categoryId);

    TransactionResponse updateTransaction(Long userId, Long id, TransactionRequest request);

    void deleteTransaction(Long userId, Long transactionId);

    void deleteAllTransactions(Long userId);

    UserCorrectionResponse addCorrection(Long userId, Long transactionId, UserCorrectionRequest request);

    UserCorrectionResponse getCorrection(Long userId, Long transactionId);

    List<TransactionResponse> getRecurringTransactions(Long userId);

    List<TransactionResponse> getAnomalyTransactions(Long userId);
    BigDecimal getTotalSpent(Long userId, LocalDate startDate, LocalDate endDate);
    BigDecimal getTransactionCount(Long userId);

    List<TransactionResponse> getRecentTransactions(Long userId);

    List<TransactionResponse> getTransactionsByCategory(Long userId, Long categoryId);

}
