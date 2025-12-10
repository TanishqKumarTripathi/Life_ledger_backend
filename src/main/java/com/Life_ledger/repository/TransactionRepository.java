package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import org.springframework.data.repository.query.Param;

import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        boolean existsByReference(String reference);

        List<Transaction> findAllByBankAccount_User_Id(Long userId);

        @Query("SELECT t FROM Transaction t WHERE t.bankAccount.user.id = :userId")
        List<Transaction> findAllByUserId(Long userId);

        Optional<Transaction> findByReferenceAndBankAccountId(String reference, Long bankAccountId);

        boolean existsByFingerprintAndBankAccountId(String fingerprint, Long bankAccountId);

        List<Transaction> findAllByBankAccountId(Long bankAccountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.bankAccount.user.id = :userId AND t.typeTransaction = 'DEBIT'")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.bankAccount.user.id = :userId")
    BigDecimal getTransactionCount(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.bankAccount.user.id = :userId ORDER BY t.date DESC LIMIT 5")
    List<Transaction> findRecentTransactionsByUserId(@Param("userId") Long userId);

        // Filter by account + date
        List<Transaction> findByBankAccount_User_IdAndBankAccount_IdAndDateAfter(
                        Long userId,
                        Long bankAccountId,
                        LocalDate date);

        @Query("SELECT t FROM Transaction t WHERE t.bankAccount.id = :accountId")
        List<Transaction> findAllByAccount(Long accountId);

        @Query("SELECT MONTH(t.date), SUM(t.amount) FROM Transaction t " +
                        "WHERE t.bankAccount.id = :accountId " +
                        "GROUP BY MONTH(t.date), YEAR(t.date)")
        List<Object[]> getMonthlyTotals(Long accountId);

        @Query("SELECT t.category.name, COUNT(t), SUM(t.amount) FROM Transaction t " +
                        "WHERE t.bankAccount.id = :accountId AND t.category IS NOT NULL " +
                        "GROUP BY t.category.name")
        List<Object[]> getCategoryTotals(Long accountId);

    @Query("SELECT t.merchant, COUNT(t), SUM(t.amount) FROM Transaction t " +
            "WHERE t.bankAccount.id = :accountId " +
            "GROUP BY t.merchant")
    List<Object[]> getMerchantTotals(Long accountId);

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
    List<Transaction> findByMerchantAndAmountRange(@Param("userId") Long userId, @Param("merchant") String merchant, @Param("amount") BigDecimal amount, @Param("tolerance") BigDecimal tolerance);

    Optional<Transaction> findByIdAndBankAccount_User_Id(Long id, Long userId);

    List<Transaction> findByCategory(Category category);

        // OR (if you have categoryId)
    List<Transaction> findByCategory_Id(Long categoryId);

    @Query("SELECT t FROM Transaction t WHERE t.category.id = :categoryId")
    List<Transaction> findAllByCategoryId(@Param("categoryId") Long categoryId);

    List<Transaction> findByCategory_IdAndBankAccount_User_Id(Long categoryId, Long userId);

    List<Transaction> findAllByBankAccount_IdAndBankAccount_User_Id(
                        Long bankAccountId,
                        Long userId);

        // Filter by user + month + year
        @Query("""
                            SELECT t FROM Transaction t
                            WHERE t.bankAccount.user.id = :userId
                            AND MONTH(t.date) = :month
                            AND YEAR(t.date) = :year
                            ORDER BY t.date DESC
                        """)
        List<Transaction> findByUserAndMonth(
                        @Param("userId") Long userId,
                        @Param("month") int month,
                        @Param("year") int year);

        // Filter + sort dynamically
        List<Transaction> findByBankAccount_User_Id(Long userId, org.springframework.data.domain.Sort sort);

        List<Transaction> findByBankAccount_User_IdAndDateBetween(
                        Long userId,
                        LocalDate from,
                        LocalDate to);

}

//    void deleteByBankAccountId(Long bankAccountId);