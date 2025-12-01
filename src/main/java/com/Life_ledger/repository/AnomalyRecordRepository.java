package com.Life_ledger.repository;

import com.Life_ledger.entity.AnomalyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalyRecordRepository extends JpaRepository<AnomalyRecord, Long> {

    // Fetch all anomalies for a specific user
    List<AnomalyRecord> findByUserId(Long userId);

    // Fetch anomalies for a specific transaction (optional but useful)
    List<AnomalyRecord> findByTransactionId(Long transactionId);

}
