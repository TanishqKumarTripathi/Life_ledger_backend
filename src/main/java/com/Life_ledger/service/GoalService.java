package com.Life_ledger.service;

import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GoalService {

    GoalResponse createGoal(GoalRequest request);

    GoalResponse updateGoal(Long goalId, GoalRequest request);

    GoalResponse getGoalById(Long goalId);

    List<GoalResponse> getAllGoals();

    List<GoalResponse> getGoalsByUser(Long userId);

    void deleteGoal(Long goalId);

    Optional<String> addContribution(Long goalId, BigDecimal amount);

    Optional<String> evaluateNudge(Long goalId);
}
