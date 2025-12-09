package com.Life_ledger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.RecurringPattern;

public interface RecurringPatternRepository extends JpaRepository<RecurringPattern, Long> {
    List<RecurringPattern> findByBankAccount_Id(Long bankAccountId);

    List<RecurringPattern> findByBankAccount_User_Id(Long userId);

    boolean existsByBankAccount_IdAndMerchantIgnoreCase(Long bankAccountId, String merchant);
}
