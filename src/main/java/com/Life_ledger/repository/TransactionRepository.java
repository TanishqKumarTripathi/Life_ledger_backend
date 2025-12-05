package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.Life_ledger.entity.Transaction;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByReference(String reference);

    // Fetch all transactions where bankAccount.user.id = :userId
    List<Transaction> findAllByBankAccount_User_Id(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.bankAccount.user.id = :userId")
    List<Transaction> findAllByUserId(Long userId);

    Optional<Transaction> findByReferenceAndBankAccountId(String reference, Long bankAccountId);

    boolean existsByFingerprintAndBankAccountId(String fingerprint, Long bankAccountId);

    @Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM Transaction t
    JOIN BankAccount b ON t.bankAccount.id = b.id
    WHERE b.user.id = :userId
      AND t.typeTransaction = 'DEBIT'
      AND (:startDate IS NULL OR t.date >= :startDate)
      AND (:endDate IS NULL OR t.date <= :endDate)
""")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId,
                                   @Param("startDate") java.time.LocalDate startDate,
                                   @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.bankAccount.user.id = :userId AND t.typeTransaction = 'DEBIT'")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.bankAccount.user.id = :userId")
    BigDecimal getTransactionCount(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.bankAccount.user.id = :userId ORDER BY t.date DESC LIMIT 5")
    List<Transaction> findRecentTransactionsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT t FROM Transaction t
    WHERE t.bankAccount.user.id = :userId
    ORDER BY t.date DESC
    """)
    List<Transaction> findRecentTransactions(@Param("userId") Long userId, Pageable pageable);

    @Query("""
    SELECT t FROM Transaction t 
    WHERE t.bankAccount.user.id = :userId 
    AND LOWER(t.merchant) LIKE LOWER(CONCAT('%', :merchant, '%'))
    AND ABS(t.amount - :amount) <= :tolerance
    ORDER BY t.date DESC
    """)
    List<Transaction> findByMerchantAndAmountRange(@Param("userId") Long userId, 
                                                  @Param("merchant") String merchant, 
                                                  @Param("amount") BigDecimal amount, 
                                                  @Param("tolerance") BigDecimal tolerance);
}