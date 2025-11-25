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

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Category name cannot be empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        // Check duplicate category for THIS user
        categoryRepository.findByUser_IdAndNameIgnoreCase(userId, request.getName())
                .ifPresent(c -> {
                    throw new RuntimeException("Category already exists");
                });

        Category category = Category.builder()
                .name(request.getName().trim())
                .user(user) // ★ IMPORTANT – assign user
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse getCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to category");
        }

        return CategoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories(Long userId) {
        return categoryRepository.findAllByUser_Id(userId)
                .stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse updateCategory(Long userId, Long categoryId, CategoryRequest request) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {

            // Duplicate name check
            categoryRepository.findByUser_IdAndNameIgnoreCase(userId, request.getName())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(categoryId)) {
                            throw new RuntimeException("Category already exists");
                        }
                    });

            category.setName(request.getName());
        }

        Category updated = categoryRepository.save(category);
        return CategoryMapper.toResponse(updated);
    }

    @Override
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        categoryRepository.delete(category);
    }
}
