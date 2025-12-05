package com.Life_ledger.controller;

import com.Life_ledger.dto.analytics.CategorySpendingResponse;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TransactionRepository transactionRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private User getUserFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    @GetMapping("/category-spending")
    public ResponseEntity<List<CategorySpendingResponse>> getCategoryWiseSpending(
            @RequestHeader("Authorization") String token) {
        
        User user = getUserFromToken(token);
        List<Object[]> results = transactionRepository.getCategoryWiseSpending(user.getId());
        
        List<CategorySpendingResponse> response = results.stream()
                .map(result -> new CategorySpendingResponse(
                        (String) result[0],
                        (BigDecimal) result[1]
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
}