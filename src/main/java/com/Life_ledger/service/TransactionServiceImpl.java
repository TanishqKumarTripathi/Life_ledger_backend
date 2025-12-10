package com.Life_ledger.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.Life_ledger.dto.transaction.*;
import com.Life_ledger.dto.usercorrection.UserCorrectionRequest;
import com.Life_ledger.dto.usercorrection.UserCorrectionResponse;
import com.Life_ledger.entity.*;
import com.Life_ledger.mapper.TransactionMapper;
import com.Life_ledger.repository.*;
import com.Life_ledger.service.TransactionService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CategoryRepository categoryRepository;
    private final UserCorrectionRepository userCorrectionRepository;
    private final AnomalyRecordRepository anomalyRecordRepository;
    private final TransactionMapper transactionMapper;

    private void checkUserOwnership(Long userId, BankAccount account) {
        if (!account.getUser().getId().equals(userId))
            throw new RuntimeException("Unauthorized");
    }

    @Override
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        BankAccount account = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        checkUserOwnership(userId, account);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Transaction txn = Transaction.builder()
                .merchant(request.getMerchant())
                .amount(request.getAmount())
                .date(request.getDate())
                .notes(request.getNotes())
                .recurring(request.isRecurring())
                .anomaly(request.isAnomaly())
                .bankAccount(account)
                .category(category)
                .build();

        Transaction saved = transactionRepository.save(txn);
        return transactionMapper.toResponse(saved);
    }

    @Override
    public TransactionResponse getTransaction(Long userId, Long id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        checkUserOwnership(userId, txn.getBankAccount());
        return transactionMapper.toResponse(txn);
    }

    @Override
    public List<TransactionResponse> getAllTransactions(Long userId, Long bankAccountId, Long categoryId) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getBankAccount() != null && t.getBankAccount().getUser() != null
                        && t.getBankAccount().getUser().getId().equals(userId))
                .filter(t -> bankAccountId == null || t.getBankAccount().getId().equals(bankAccountId))
                .filter(t -> categoryId == null || (t.getCategory() != null && t.getCategory().getId().equals(categoryId)))
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponse updateTransaction(Long userId, Long id, TransactionRequest request) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        checkUserOwnership(userId, txn.getBankAccount());

        if (request.getMerchant() != null)
            txn.setMerchant(request.getMerchant());
        if (request.getAmount() != null)
            txn.setAmount(request.getAmount());
        if (request.getDate() != null)
            txn.setDate(request.getDate());
        if (request.getNotes() != null)
            txn.setNotes(request.getNotes());
        txn.setRecurring(request.isRecurring());
        txn.setAnomaly(request.isAnomaly());

        return transactionMapper.toResponse(transactionRepository.save(txn));
    }

    @Override
    @Transactional
    public void deleteTransaction(Long userId, Long id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        checkUserOwnership(userId, txn.getBankAccount());
        
        // Delete related anomaly records first
        anomalyRecordRepository.deleteByTransactionId(id);
        
        transactionRepository.delete(txn);
    }

    @Override
    public UserCorrectionResponse addCorrection(Long userId, Long transactionId, UserCorrectionRequest request) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        checkUserOwnership(userId, txn.getBankAccount());

        UserCorrection correction = UserCorrection.builder()
                .transaction(txn)
                .correctedMerchant(request.getCorrectedMerchant())
                .correctedAmount(request.getCorrectedAmount())
                .notes(request.getNotes())
                .build();

        UserCorrection saved = userCorrectionRepository.save(correction);
        txn.setCorrection(saved);
        transactionRepository.save(txn);

        return UserCorrectionResponse.builder()
                .id(saved.getId())
                .correctedMerchant(saved.getCorrectedMerchant())
                .correctedAmount(saved.getCorrectedAmount())
                .notes(saved.getNotes())
                .transactionId(txn.getId())
                .build();
    }

    @Override
    public UserCorrectionResponse getCorrection(Long userId, Long transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        checkUserOwnership(userId, txn.getBankAccount());

        if (txn.getCorrection() == null)
            throw new RuntimeException("No correction found for this transaction");

        UserCorrection c = txn.getCorrection();
        return UserCorrectionResponse.builder()
                .id(c.getId())
                .correctedMerchant(c.getCorrectedMerchant())
                .correctedAmount(c.getCorrectedAmount())
                .notes(c.getNotes())
                .transactionId(txn.getId())
                .build();
    }

    @Override
    public List<TransactionResponse> getRecurringTransactions(Long userId) {
        return transactionRepository.findAll().stream().filter(t -> t.getBankAccount().getUser().getId().equals(userId)).
        filter(Transaction::isRecurring)
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> getAnomalyTransactions(Long userId) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getBankAccount().getUser().getId().equals(userId))
                .filter(Transaction::isAnomaly)
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalSpent(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.getTotalSpentByUser(userId);
    }

    @Override
    public BigDecimal getTransactionCount(Long userId) {
        return transactionRepository.getTransactionCount(userId);
    }

    @Override
    public List<TransactionResponse> getRecentTransactions(Long userId) {
        return transactionRepository.findRecentTransactionsByUserId(userId).stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAllTransactions(Long userId) {
        List<Transaction> transactions = transactionRepository.findAllByBankAccount_User_Id(userId);
        
        // Delete all anomaly records for these transactions
        List<Long> transactionIds = transactions.stream()
                .map(Transaction::getId)
                .collect(Collectors.toList());
        
        if (!transactionIds.isEmpty()) {
            anomalyRecordRepository.deleteByTransactionIdIn(transactionIds);
        }
        
        transactionRepository.deleteAll(transactions);
    }

    public List<Transaction> getTransactionsByCategory(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return transactionRepository.findByCategory(category);
    }

    @Override
    public List<TransactionResponse> getTransactionsByCategory(Long userId, Long categoryId) {

        // ✅ Ownership check (important)
        boolean categoryExists = categoryRepository
                .existsByIdAndUser_Id(categoryId, userId);

        if (!categoryExists) {
            throw new RuntimeException("Category not found or unauthorized");
        }

        return transactionRepository
                .findByCategory_IdAndBankAccount_User_Id(categoryId, userId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

}
