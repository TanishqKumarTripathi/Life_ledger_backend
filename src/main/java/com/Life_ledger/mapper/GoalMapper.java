package com.Life_ledger.mapper;

import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.entity.Goal;

import java.math.BigDecimal;

public class GoalMapper {

    public static Goal toEntity(GoalRequest req) {
        return Goal.builder()
                .name(req.getName())
                .targetAmount(req.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .type(req.getType())
                .category(req.getCategory())
                .startDate(req.getStartDate())
                .deadline(req.getDeadline())
                .build();
    }

    public static GoalResponse toResponse(Goal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .category(goal.getCategory())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .deadline(goal.getDeadline())
                .startDate(goal.getStartDate())
                .type(goal.getType())
                .progressPercent(
                        goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                                ? goal.getCurrentAmount().doubleValue() / goal.getTargetAmount().doubleValue() * 100
                                : 0.0)
                .build();
    }

    public static GoalResponse toDto(Goal goal) {
        if (goal == null)
            return null;

        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .build();
    }
}
