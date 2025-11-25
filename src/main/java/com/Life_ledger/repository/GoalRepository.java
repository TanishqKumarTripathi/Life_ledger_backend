package com.Life_ledger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findAllByUserId(Long userId);
}
