package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long> {

}
