package com.Life_ledger.dto.goals;

import com.Life_ledger.enums.GoalType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {
    private Long user_id;
    private String name;
    private BigDecimal targetAmount;
    private LocalDate startDate;
    private LocalDate deadline;
    private GoalType type;
    private String category;

}
