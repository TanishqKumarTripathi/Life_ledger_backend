package com.Life_ledger.dto.insight;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AnomalyDTO {
    private Long transactionId;
    private String merchant;
    private BigDecimal amount;
    private LocalDate date;
    private String reason;
    private String anomalyType;
    private Double confidence;
}