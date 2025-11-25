package com.Life_ledger.mapper;

import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.entity.Goal;

public class GoalMapper {

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
