package com.Life_ledger.service;

import com.Life_ledger.dto.category.CategoryRequest;
import com.Life_ledger.dto.category.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(Long userId, CategoryRequest request);
    
    CategoryResponse createCategoryForAccount(Long accountId, CategoryRequest request);

    CategoryResponse getCategory(Long userId, Long categoryId);

    List<CategoryResponse> getAllCategories(Long userId);
    
    List<CategoryResponse> getCategoriesByAccount(Long accountId);

    CategoryResponse updateCategory(Long userId, Long categoryId, CategoryRequest request);

    void deleteCategory(Long userId, Long categoryId);
}
