package com.Life_ledger.controller;

import com.Life_ledger.dto.transaction.*;
import com.Life_ledger.dto.usercorrection.*;
import com.Life_ledger.entity.User;
import com.Life_ledger.service.TransactionService;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Life_ledger.repository.TransactionRepository;


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

    @GetMapping("/total-spent")
    public ResponseEntity <Map<String,Object>> getTotalSpent(@RequestHeader("Authorization") String token,
            @RequestParam(required =false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam (required =false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        User user = getUserFromToken(token);
        BigDecimal totalSpent = transactionService.getTotalSpent(user.getId(), startDate, endDate);
        return ResponseEntity.ok(Map.of("totalSpent", totalSpent,
                                        "startDate", startDate,
                                        "endDate", endDate));
            }

    @GetMapping("/count")
    public ResponseEntity <Map<String,Object>> getTransactionCount(@RequestHeader("Authorization") String token){
        User user =getUserFromToken(token);
        BigDecimal TransactionCount = transactionService.getTransactionCount(user.getId());
        return TransactionRepository.getTrasantionCount(user.getId());
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
