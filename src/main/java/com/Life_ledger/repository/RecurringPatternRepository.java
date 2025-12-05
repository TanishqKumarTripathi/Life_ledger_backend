package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Life_ledger.entity.RecurringPattern;

import java.util.List;

public interface RecurringPatternRepository extends JpaRepository<RecurringPattern, Long> {

    @Query("SELECT rp FROM RecurringPattern rp " +
           "LEFT JOIN FETCH rp.transaction t " +
           "LEFT JOIN FETCH t.bankAccount ba " +
           "WHERE ba.user.id = :userId")
    List<RecurringPattern> findByUserIdWithBankAccount(@Param("userId") Long userId);

    @Query("SELECT rp FROM RecurringPattern rp " +
           "LEFT JOIN FETCH rp.transaction t " +
           "LEFT JOIN FETCH t.bankAccount")
    List<RecurringPattern> findAllWithBankAccount();
}