package com.Life_ledger.mapper;

import com.Life_ledger.dto.category.CategoryResponse;
import com.Life_ledger.entity.Category;

public class CategoryMapper {
    public static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
