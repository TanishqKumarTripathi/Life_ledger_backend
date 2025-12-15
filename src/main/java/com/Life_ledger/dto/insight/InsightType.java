package com.Life_ledger.dto.insight;

public enum InsightType {
    SUMMARY, // Monthly / period overview
    ANOMALY, // Spending irregularities (optional if you store separately)
    RECURRING, // Subscription / EMI insights (optional)
    BUDGET, // Future: budget-related insights
    SYSTEM // Debug / system generated (optional)
}
