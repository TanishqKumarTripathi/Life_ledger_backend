package com.Life_ledger.dto.goals;

import com.Life_ledger.entity.Goal;
import com.Life_ledger.Enum.GoalStatus;
import com.Life_ledger.Enum.GoalType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@Builder
public class GoalResponse {
    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate startDate;
    private LocalDate deadline;
    private GoalType type;
    private GoalStatus status;
    private String category;
    private Double nudgeThreshold;
    private Double progressPercent;
    private Long daysRemaining;
    private Boolean isOverdue;

    public static GoalResponse from(Goal goal) {
        Double progress = 0.0;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        Long daysRemaining = null;
        Boolean isOverdue = false;
        if (goal.getDeadline() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
            isOverdue = daysRemaining < 0;
        }

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
                .progressPercent(progress)
                .daysRemaining(daysRemaining)
                .isOverdue(isOverdue)
                .build();
    }
}