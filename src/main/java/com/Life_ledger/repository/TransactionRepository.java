package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.Life_ledger.entity.Transaction;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByReference(String reference);

    // Fetch all transactions where bankAccount.user.id = :userId
    List<Transaction> findAllByBankAccount_User_Id(Long userId);
}
