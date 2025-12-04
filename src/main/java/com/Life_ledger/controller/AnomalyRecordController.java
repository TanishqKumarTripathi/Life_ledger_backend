package com.Life_ledger.controller;

import com.Life_ledger.entity.AnomalyRecord;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.AnomalyRecordService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyRecordController {

    private final AnomalyRecordService anomalyService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    // --------------------------------------------------
    // Helper: extract user from JWT
    // --------------------------------------------------
    private User getUser(String tokenHeader) {
        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtil.extractUsername(token);

            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token");
        }
    }

    // --------------------------------------------------
    // Validate that account belongs to the logged-in user
    // --------------------------------------------------
    private BankAccount validateAccountOwner(Long accountId, Long userId) {
        BankAccount acc = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!acc.getUser().getId().equals(userId))
            throw new RuntimeException("Access denied: Account does not belong to user");

        return acc;
    }

    // --------------------------------------------------
    // GET ALL anomalies for a specific bank account
    // --------------------------------------------------
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getAllByAccount(
            @RequestHeader("Authorization") String token,
            @PathVariable Long accountId) {

        try {
            User user = getUser(token);
            validateAccountOwner(accountId, user.getId());

            List<AnomalyRecord> anomalies = anomalyService.getAnomaliesByBankAccount(accountId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", anomalies.size(),
                    "anomalies", anomalies));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    // --------------------------------------------------
    // GET a single anomaly (account owner only)
    // --------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        try {
            User user = getUser(token);
            AnomalyRecord anomaly = anomalyService.getAnomaly(id);

            if (!anomaly.getBankAccount().getUser().getId().equals(user.getId()))
                return ResponseEntity.status(403).body(Map.of("status", "error", "message", "Access denied"));

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "anomaly", anomaly));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    // --------------------------------------------------
    // DELETE anomaly (only account owner)
    // --------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        try {
            User user = getUser(token);
            AnomalyRecord anomaly = anomalyService.getAnomaly(id);

            if (!anomaly.getBankAccount().getUser().getId().equals(user.getId()))
                return ResponseEntity.status(403).body(Map.of("status", "error", "message", "Access denied"));

            anomalyService.deleteAnomaly(id);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Anomaly deleted"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    // --------------------------------------------------
    // MARK anomaly resolved
    // --------------------------------------------------
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        try {
            User user = getUser(token);
            AnomalyRecord anomaly = anomalyService.getAnomaly(id);

            if (!anomaly.getBankAccount().getUser().getId().equals(user.getId()))
                return ResponseEntity.status(403).body(Map.of("status", "error", "message", "Access denied"));

            boolean resolved = (boolean) body.getOrDefault("resolved", false);
            String comment = (String) body.getOrDefault("comment", "");

            AnomalyRecord updated = anomalyService.markResolved(id, resolved, comment);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Anomaly updated",
                    "anomaly", updated));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }
}
