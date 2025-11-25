package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFileImportId(Long fileImportId);
    List<Transaction> findByMerchantAndAmount(String merchant, BigDecimal amount);

    long countByFileImportId(Long fileImportId);
}
