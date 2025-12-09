package com.Life_ledger.dto.gemini;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level JSON from Gemini.
 * Expected JSON:
 * {
 * "transactions": [ ... GeminiTransactionDto ... ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiOcrResponseDto {
    private List<GeminiTransactionDto> transactions;
}
