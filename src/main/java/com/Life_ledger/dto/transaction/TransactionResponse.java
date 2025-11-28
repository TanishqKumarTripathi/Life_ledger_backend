package com.Life_ledger.dto.transaction;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String merchant;
    private BigDecimal amount;
    private LocalDate date;
    private String notes;
    private boolean recurring;
    private boolean anomaly;
    private String bankAccountLast4;
    private String categoryName;
    private String subCategoryName;
}