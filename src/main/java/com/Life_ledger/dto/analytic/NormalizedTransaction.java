package com.Life_ledger.dto.analytic;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class NormalizedTransaction {
    private Long id;
    private String cleanMerchant;
    private String originalMerchant;
    private BigDecimal signedAmount; // negative for expenses, positive for income
    private String category;
    private String subCategory;
    private LocalDate date;
    private boolean isExpense;
    private boolean isRecurring;
    private String reference;
    private String notes;
}