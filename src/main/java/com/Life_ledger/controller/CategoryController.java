package com.Life_ledger.controller;

import com.Life_ledger.dto.category.CategoryRequest;
import com.Life_ledger.dto.category.CategoryResponse;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private User getUserFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestHeader("Authorization") String token,
            @RequestBody CategoryRequest request) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(categoryService.createCategory(user.getId(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(categoryService.getCategory(user.getId(), id));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            @RequestHeader("Authorization") String token) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(categoryService.getAllCategories(user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(categoryService.updateCategory(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        User user = getUserFromToken(token);
        categoryService.deleteCategory(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
