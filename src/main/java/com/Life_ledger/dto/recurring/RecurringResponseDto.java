package com.Life_ledger.dto.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecurringResponseDto {
    private Long id;
    private String merchant;
    private BigDecimal amount;
    private String frequency;
    private LocalDate nextDueDate;
}

