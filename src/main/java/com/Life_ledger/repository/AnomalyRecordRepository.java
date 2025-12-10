package com.Life_ledger.repository;

import com.Life_ledger.entity.AnomalyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AnomalyRecordRepository extends JpaRepository<AnomalyRecord, Long> {

    List<AnomalyRecord> findByBankAccount_Id(Long bankAccountId);
    List<AnomalyRecord> findByTransaction_Id(Long transactionId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM AnomalyRecord a WHERE a.transaction.id = :transactionId")
    void deleteByTransactionId(@Param("transactionId") Long transactionId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM AnomalyRecord a WHERE a.transaction.id IN :transactionIds")
    void deleteByTransactionIdIn(@Param("transactionIds") List<Long> transactionIds);
}
