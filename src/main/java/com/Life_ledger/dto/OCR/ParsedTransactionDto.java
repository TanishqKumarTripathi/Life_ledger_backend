package com.Life_ledger.dto.OCR;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.Life_ledger.Enum.TransactionEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Data
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedTransactionDto {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private TransactionEnum type; // CREDIT / DEBIT
}
