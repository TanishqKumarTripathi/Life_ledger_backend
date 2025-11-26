package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.Life_ledger.entity.Transaction;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByReference(String reference);

    // Fetch all transactions where bankAccount.user.id = :userId
    List<Transaction> findAllByBankAccount_User_Id(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.bankAccount.user.id = :userId")
    List<Transaction> findAllByUserId(Long userId);

    Optional<Transaction> findByReferenceAndBankAccountId(String reference, Long bankAccountId);

}
