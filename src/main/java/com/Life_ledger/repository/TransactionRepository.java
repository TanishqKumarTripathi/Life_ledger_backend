package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("Select COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.bankAccount.user.id = :userId " +"AND t.typeTransaction='DEBIT' " +"AND (:startDate IS NULL or t.date>= :startDate)" + "AND (:endDate IS NULL or t.date<= :endDate)")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId,
                                   @Param("startDate") java.time.LocalDate startDate,
                                   @Param("endDate") java.time.LocalDate endDate);

    @Query("Select COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.bankAccount.user.id = :userId AND t.typeTransaction='DEBIT' " )
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);
    BigDecimal getTransactionCount(Long userId);
}
