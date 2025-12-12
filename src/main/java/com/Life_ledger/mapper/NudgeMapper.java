package com.Life_ledger.mapper;

import com.Life_ledger.dto.nudge.NudgeDto;
import com.Life_ledger.entity.Nudge;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class NudgeMapper {

    public NudgeDto toDto(Nudge n) {

        BigDecimal spent = n.getGoal().getCurrentAmount();
        BigDecimal target = n.getGoal().getTargetAmount();

        BigDecimal remaining = target.subtract(spent).max(BigDecimal.ZERO);
        BigDecimal exceeded = spent.compareTo(target) > 0
                ? spent.subtract(target)
                : BigDecimal.ZERO;

        Double progress = spent
                .multiply(BigDecimal.valueOf(100))
                .divide(target, 2, RoundingMode.HALF_UP)
                .doubleValue();

        return NudgeDto.builder()
                .id(n.getId())
                .goalId(n.getGoal() != null ? n.getGoal().getId() : null)
                .goalName(n.getGoal() != null ? n.getGoal().getName() : null)
                .category(n.getCategory())
                .type(n.getType())
                .severity(n.getSeverity())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .spentAmount(spent)
                .targetAmount(target)
                .remainingAmount(remaining)
                .exceededAmount(exceeded)
                .progressPercent(progress)

                .build();
    }
}
