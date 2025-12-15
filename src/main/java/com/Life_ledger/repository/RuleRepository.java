package com.Life_ledger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Life_ledger.entity.Rule;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByUser_IdOrderByPriorityAsc(Long userId);
}