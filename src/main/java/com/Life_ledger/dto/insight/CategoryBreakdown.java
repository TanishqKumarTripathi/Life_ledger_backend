package com.Life_ledger.dto.insight;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategoryBreakdown {
    private String categoryName;
    private BigDecimal amount;
    private Double percentage;
    private int transactionCount;
}
