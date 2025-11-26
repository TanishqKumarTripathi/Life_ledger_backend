package com.Life_ledger.dto.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RuleResponse {

    private Long id;

    private String keyword;
    private String regex;

    private Double minAmount;
    private Double maxAmount;

    private Integer priority;

    private Long categoryId;
    private String categoryName;

    private Long subCategoryId;
    private String subCategoryName;
}
