package com.Life_ledger.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.entity.Goal;
import com.Life_ledger.Enum.GoalStatus;
import com.Life_ledger.Enum.GoalType;

@Component
public class GoalMapper {

    public Goal toEntity(GoalRequest req) {
        return Goal.builder()
                .name(req.getName())
                .targetAmount(req.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .category(req.getCategory())
                .startDate(LocalDate.now())
                .deadline(req.getDeadline())
                .type(GoalType.valueOf(req.getType().toUpperCase()))
                .status(GoalStatus.ACTIVE)
                .nudgeThreshold(req.getNudgeThreshold() != null ? req.getNudgeThreshold() : 0.8)
                .build();
    }

    public void updateGoalFromRequest(Goal goal, GoalRequest req) {
        if (req.getName() != null)
            goal.setName(req.getName());
        if (req.getTargetAmount() != null)
            goal.setTargetAmount(req.getTargetAmount());
        if (req.getDeadline() != null)
            goal.setDeadline(req.getDeadline());
        if (req.getCategory() != null)
            goal.setCategory(req.getCategory());
        if (req.getNudgeThreshold() != null)
            goal.setNudgeThreshold(req.getNudgeThreshold());
        if (req.getType() != null)
            goal.setType(GoalType.valueOf(req.getType().toUpperCase()));
    }

    public static GoalResponse toDto(Goal goal) {
        if (goal == null)
            return null;

        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .startDate(goal.getStartDate())
                .deadline(goal.getDeadline())
                .type(goal.getType())
                .status(goal.getStatus())
                .category(goal.getCategory())
                .nudgeThreshold(goal.getNudgeThreshold())
                .build();
    }
}