package com.Life_ledger.service;

import com.Life_ledger.dto.category.CategoryRequest;
import com.Life_ledger.dto.category.CategoryResponse;
import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.CategoryMapper;
import com.Life_ledger.repository.CategoryRepository;
import com.Life_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public CategoryResponse createCategory(Long userId, CategoryRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        Category category = Category.builder()
                .name(request.getName())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse getCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return CategoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories(Long userId) {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse updateCategory(Long userId, Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (request.getName() != null) {
            category.setName(request.getName());
        }

        Category updated = categoryRepository.save(category);
        return CategoryMapper.toResponse(updated);
    }

    @Override
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        categoryRepository.delete(category);
    }
}
