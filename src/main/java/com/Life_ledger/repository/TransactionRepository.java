package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.Life_ledger.entity.Transaction;

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

    List<Transaction> findByBankAccount_User_Id(Long userId);

    // Filter by date ONLY (all accounts)
    List<Transaction> findByBankAccount_User_IdAndDateAfter(Long userId, LocalDate date);

    // ❌ REMOVE → WRONG → DO NOT KEEP
    // List<Transaction> findByUserIdAndBankAccountIdAndDateAfter(...);

    // Filter by account + date
    List<Transaction> findByBankAccount_User_IdAndBankAccount_IdAndDateAfter(
            Long userId,
            Long bankAccountId,
            LocalDate date);
}
