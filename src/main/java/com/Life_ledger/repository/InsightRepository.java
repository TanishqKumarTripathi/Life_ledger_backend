package com.Life_ledger.repository;

import com.Life_ledger.dto.insight.InsightType;
import com.Life_ledger.entity.Insight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsightRepository extends JpaRepository<Insight, Long> {

    // Fetch insights for a specific bank account
    List<Insight> findByBankAccount_Id(Long bankAccountId);

    void deleteByBankAccount_IdAndTypeAndPeriod(
            Long bankAccountId,
            InsightType type,
            String period);

    Optional<Insight> findTopByBankAccount_IdAndTypeOrderByCreatedAtDesc(
            Long bankAccountId,
            InsightType type);
}
