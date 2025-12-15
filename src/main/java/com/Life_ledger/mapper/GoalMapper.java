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
                .startDate(req.getStartDate())
                .deadline(req.getDeadline())
                .type(req.getType()) // ✅ FIX
                .status(GoalStatus.ACTIVE)
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

        if (req.getType() != null)
            goal.setType(req.getType()); // ✅ FIX
    }
}
