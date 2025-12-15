package com.Life_ledger.dto.insight;

import lombok.Data;

@Data
public class InsightRequestDTO {

    private String aiText;

    // 🔥 REQUIRED after converting Insight to bank-account-based
    private Long bankAccountId;
}
