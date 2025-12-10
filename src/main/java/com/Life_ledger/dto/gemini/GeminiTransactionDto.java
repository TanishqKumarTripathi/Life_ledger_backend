package com.Life_ledger.dto.gemini;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.Life_ledger.Enum.TransactionEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One transaction returned by Gemini OCR JSON.
 * Expected JSON shape:
 * {
 * "date": "2025-05-01",
 * "description": "...",
 * "amount": 123.45,
 * "type": "DEBIT", // or "CREDIT"
 * "reference": "some-id-or-null"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiTransactionDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date; // ISO string: 2025-05-01
    private String description;
    private BigDecimal amount;
    private TransactionEnum type;
    private String reference;
}
