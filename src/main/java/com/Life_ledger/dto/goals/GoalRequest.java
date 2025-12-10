package com.Life_ledger.dto.goals;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.Life_ledger.Enum.GoalType;

@Data
public class GoalRequest {
    private Long userId;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate startDate;
    private LocalDate deadline;
    private GoalType type;
    private String category;
}