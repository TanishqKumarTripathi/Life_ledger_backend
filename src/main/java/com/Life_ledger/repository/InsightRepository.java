package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Insight;

public interface InsightRepository extends JpaRepository<Insight, Long> {
}
