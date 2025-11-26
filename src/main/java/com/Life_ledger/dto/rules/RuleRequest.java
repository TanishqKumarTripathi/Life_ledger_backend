package com.Life_ledger.dto.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RuleRequest {
    private String keyword;
    private String regex;
    private Double minAmount;
    private Double maxAmount;
    private Integer priority;

    // If present, server will create/find these for the user
    private String categoryName;
    private String subCategoryName;
}
