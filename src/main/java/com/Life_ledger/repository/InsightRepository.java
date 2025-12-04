package com.Life_ledger.repository;

import com.Life_ledger.entity.Insight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsightRepository extends JpaRepository<Insight, Long> {

    // Fetch insights for a specific bank account
    List<Insight> findByBankAccount_Id(Long bankAccountId);
}
