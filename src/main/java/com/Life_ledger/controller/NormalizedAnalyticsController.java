package com.Life_ledger.controller;

import com.Life_ledger.dto.analytic.NormalizedTransaction;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.TransactionNormalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics/normalized")
@RequiredArgsConstructor
public class NormalizedAnalyticsController {

    private final TransactionNormalizationService normalizationService;
    private final TransactionRepository transactionRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private User getUserFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewNormalization(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int limit) {
        
        User user = getUserFromToken(token);
        List<Transaction> rawTransactions = transactionRepository.findAllByUserId(user.getId())
                .stream().limit(limit).collect(Collectors.toList());
        
        List<NormalizedTransaction> normalized = normalizationService.normalizeTransactions(rawTransactions);
        
        return ResponseEntity.ok(Map.of(
            "raw", rawTransactions,
            "normalized", normalized,
            "summary", Map.of(
                "totalTransactions", normalized.size(),
                "expenses", normalized.stream().filter(NormalizedTransaction::isExpense).count(),
                "income", normalized.stream().filter(t -> !t.isExpense()).count(),
                "recurring", normalized.stream().filter(NormalizedTransaction::isRecurring).count(),
                "categories", normalized.stream().map(NormalizedTransaction::getCategory).distinct().count(),
                "merchants", normalized.stream().map(NormalizedTransaction::getCleanMerchant).distinct().count()
            )
        ));
    }
}