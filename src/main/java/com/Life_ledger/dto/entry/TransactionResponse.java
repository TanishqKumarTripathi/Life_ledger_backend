package com.Life_ledger.dto.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String merchant;
    private Double amount;
    private String category;
    private Boolean recurring;
    private Boolean anomaly;
    private String notes;
}
