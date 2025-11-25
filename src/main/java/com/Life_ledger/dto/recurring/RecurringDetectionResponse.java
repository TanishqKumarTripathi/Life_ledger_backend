package com.Life_ledger.dto.recurring;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDetectionResponse {

    private String merchant;

    private BigDecimal averageAmount;

    private String frequency; // Monthly, Weekly, Yearly

    private LocalDate nextDueDate;

    private int occurrenceCount; // How many times pattern repeated

    private boolean isRecurring;
}
