package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
