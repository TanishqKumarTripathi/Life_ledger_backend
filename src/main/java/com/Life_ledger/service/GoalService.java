package com.Life_ledger.service;

import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GoalService {

    GoalResponse createGoal(GoalRequest request);

    GoalResponse createGoalForAccount(GoalRequest request, Long accountId);

    GoalResponse getGoalByIdAndUser(Long goalId, Long userId);

    List<GoalResponse> getGoalsByUser(Long userId);

    List<GoalResponse> getGoalsByAccount(Long accountId);

    GoalResponse updateGoal(Long goalId, GoalRequest request);

    void deleteGoal(Long goalId, Long userId);

    Optional<String> addContribution(Long goalId, BigDecimal amount, Long userId);

    Optional<String> evaluateNudge(Long goalId, Long userId);

    public void deleteAllGoalsByUser(Long userId);

    public void deleteAllGoalsByAccount(Long userId, Long accountId);

}
