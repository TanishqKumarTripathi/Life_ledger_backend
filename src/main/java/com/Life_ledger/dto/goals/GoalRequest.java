package com.Life_ledger.dto.goals;

import com.Life_ledger.enums.GoalType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {
    private Long userId;
    private String name;
    private BigDecimal targetAmount;
    private LocalDate deadline;
    private String type;
    private String category;
    private Double nudgeThreshold;
}