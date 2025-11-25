package com.Life_ledger.repository;

import com.Life_ledger.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.Goal;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);
}
