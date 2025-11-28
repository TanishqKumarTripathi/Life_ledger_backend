package com.Life_ledger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Insight;

public interface InsightRepository extends JpaRepository<Insight, Long> {
    List<Insight> findByUser_Id(Long userId);

    // Optional alternative naming
    List<Insight> findByUserId(Long userId);
}
