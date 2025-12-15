package com.Life_ledger.dto.nudge;

import com.Life_ledger.Enum.NudgeSeverity;
import com.Life_ledger.Enum.NudgeType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NudgeDto {

    private Long id;

    private Long goalId;
    private String goalName;
    private String category;

    private NudgeType type;
    private NudgeSeverity severity;

    private String title;
    private String message;

    private boolean read;
    private LocalDateTime createdAt;

    // ⭐ NEW FIELDS ⭐
    private BigDecimal spentAmount;
    private BigDecimal targetAmount;
    private BigDecimal remainingAmount;
    private BigDecimal exceededAmount;
    private Double progressPercent;
}
