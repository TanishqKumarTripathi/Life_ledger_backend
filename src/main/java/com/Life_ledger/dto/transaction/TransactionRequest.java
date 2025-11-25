package com.Life_ledger.dto.transaction;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {
    private String merchant;
    private BigDecimal amount;
    private LocalDate date;
    private String notes;
    private boolean recurring;
    private boolean anomaly;
    private Long bankAccountId;
    private Long categoryId;
}
