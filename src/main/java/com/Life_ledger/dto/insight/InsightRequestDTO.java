package com.Life_ledger.dto.insight;

import lombok.Data;

@Data
public class InsightRequestDTO {

    // The AI-generated text to store
    private String aiText;

    // In the future you can add: relatedTransactionIds, category, tone, etc.
}
