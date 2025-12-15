package com.Life_ledger.dto.anomaly;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
public class AnomalyResponse {
    private Long id;
    private Long transactionId;
    private String merchant;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String reason;
    private String anomalyType;
    private Double confidence;
    private LocalDateTime createdAt;
}
