package com.Life_ledger.controller;

import com.Life_ledger.service.GeminiService;
import com.Life_ledger.service.GeminiServiceImpl.Step;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final GeminiService geminiService;

    // ---------------------------------------------------------------------
    // 🔹 Extract userId from JWT (WITHOUT JwtService)
    // ---------------------------------------------------------------------
    private Long extractUserIdFromJwt(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new RuntimeException("Missing or invalid Authorization header.");

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .build() // no signature validation required since you only need to read payload
                    .parseClaimsJwt(token.split("\\.")[0] + "." + token.split("\\.")[1] + ".")
                    .getBody();

            Object uid = claims.get("userId");

            if (uid == null)
                throw new RuntimeException("userId missing in JWT");

            return Long.parseLong(uid.toString());
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 Start Multi-Step AI Analysis
    // ---------------------------------------------------------------------
    @PostMapping("/analyze")
    public ResponseEntity<?> startAnalysis(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);

            geminiService.analyzeUserTransactionsAsync(userId, accountId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "started",
                            "message", "AI multi-step analysis started",
                            "userId", userId,
                            "accountId", accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 Get Status for ALL steps
    // ---------------------------------------------------------------------
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);
            return ResponseEntity.ok(geminiService.getStatusForUserAccount(userId, accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 Get Status for a specific step
    // ---------------------------------------------------------------------
    @GetMapping("/status/{step}")
    public ResponseEntity<?> getStepStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Step step,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);
            return ResponseEntity.ok(geminiService.getStepStatusForUserAccount(userId, accountId, step));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 MANUAL: Trigger Categorization Only
    // ---------------------------------------------------------------------
    @PostMapping("/categorize")
    public ResponseEntity<?> runCategorization(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);

            geminiService.processCategorization(userId, accountId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "started",
                            "step", "categorization",
                            "userId", userId,
                            "accountId", accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 MANUAL: Trigger Recurring Only
    // ---------------------------------------------------------------------
    @PostMapping("/recurring")
    public ResponseEntity<?> runRecurring(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);

            geminiService.processRecurringAsync(userId, accountId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "started",
                            "step", "recurring",
                            "userId", userId,
                            "accountId", accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 MANUAL: Trigger Anomalies Only
    // ---------------------------------------------------------------------
    @PostMapping("/anomalies")
    public ResponseEntity<?> runAnomalies(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);

            geminiService.processAnomaliesAsync(userId, accountId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "started",
                            "step", "anomalies",
                            "userId", userId,
                            "accountId", accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 MANUAL: Trigger Summary Only
    // ---------------------------------------------------------------------
    @PostMapping("/summary")
    public ResponseEntity<?> runSummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);

            geminiService.processSummaryAsync(userId, accountId);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "started",
                            "step", "summary",
                            "userId", userId,
                            "accountId", accountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 Test endpoints
    // ---------------------------------------------------------------------
    @GetMapping("/test")
    public String test() {
        return geminiService.testModel();
    }

    @GetMapping("/models")
    public String listModels() {
        return geminiService.listModels();
    }
}
