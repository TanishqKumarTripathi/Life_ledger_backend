package com.Life_ledger.controller;

import com.Life_ledger.dto.transaction.*;
import com.Life_ledger.dto.usercorrection.*;
import com.Life_ledger.entity.User;
import com.Life_ledger.service.TransactionService;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    

    private User getUserFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestHeader("Authorization") String token,
            @RequestBody TransactionRequest request) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.createTransaction(user.getId(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.getTransaction(user.getId(), id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) Long categoryId) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.getAllTransactions(user.getId(), bankAccountId, categoryId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody TransactionRequest request) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.updateTransaction(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        User user = getUserFromToken(token);
        transactionService.deleteTransaction(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/correction")
    public ResponseEntity<UserCorrectionResponse> addCorrection(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody UserCorrectionRequest request) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.addCorrection(user.getId(), id, request));
    }

    @GetMapping("/{id}/correction")
    public ResponseEntity<UserCorrectionResponse> getCorrection(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.getCorrection(user.getId(), id));
    }

    @GetMapping("/recurring")
    public ResponseEntity<List<TransactionResponse>> getRecurringTransactions(
            @RequestHeader("Authorization") String token) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.getRecurringTransactions(user.getId()));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<TransactionResponse>> getAnomalyTransactions(
            @RequestHeader("Authorization") String token) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(transactionService.getAnomalyTransactions(user.getId()));
    }
}
