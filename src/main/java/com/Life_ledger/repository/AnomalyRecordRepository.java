package com.Life_ledger.repository;

import com.Life_ledger.entity.AnomalyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalyRecordRepository extends JpaRepository<AnomalyRecord, Long> {

    // Fetch anomalies for a specific bank account
    List<AnomalyRecord> findByBankAccount_Id(Long bankAccountId);

    // Optional: fetch anomalies for a specific transaction
    List<AnomalyRecord> findByTransaction_Id(Long transactionId);
}
