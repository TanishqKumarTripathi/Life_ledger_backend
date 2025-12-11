package com.Life_ledger.controller;

import com.Life_ledger.dto.anomaly.AnomalyResponse;
import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.dto.insight.InsightSummaryResponse;
import com.Life_ledger.dto.recurring.RecurringResponseDto;
import com.Life_ledger.entity.AnomalyRecord;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.Recurringmapper;
import com.Life_ledger.repository.AnomalyRecordRepository;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.RecurringPatternRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.service.AnomalyRecordService;
import com.Life_ledger.service.GeminiService;
import com.Life_ledger.service.GeminiServiceImpl.Step;
import com.Life_ledger.service.InsightService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final GeminiService geminiService;
    private final AnomalyRecordService anomalyRecordService;
    private final InsightService insightService;
    private final RecurringPatternRepository recurringPatternRepository;
    private final Recurringmapper recurringMapper;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

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
            System.out.println("Analyze endpoint received accountId = " + accountId);
            System.out.println("Analyze running for user = " + userId);

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

    // ---------------------------------------------------------------------
    // 🔹 GET: Retrieve Anomalies (from anomaly_records table)
    // ---------------------------------------------------------------------
    @GetMapping("/anomalies")
    public ResponseEntity<?> getAnomalies(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<AnomalyRecord> anomalies;
            if (accountId != null) {
                BankAccount account = bankAccountRepository.findByIdAndUser(accountId, user)
                        .orElseThrow(() -> new RuntimeException("Account not found or access denied"));
                anomalies = anomalyRecordService.getAnomaliesByBankAccount(accountId);
            } else {
                anomalies = anomalyRecordService.getAnomaliesByUser(userId);
            }
            
            List<AnomalyResponse> responses = anomalies.stream()
                    .map(this::toAnomalyResponse)
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", responses.size(),
                    "anomalies", responses
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 GET: Retrieve Recurring Patterns (from recurring_patterns table)
    // ---------------------------------------------------------------------
    @GetMapping("/recurring")
    public ResponseEntity<?> getRecurring(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<RecurringPattern> patterns;
            if (accountId != null) {
                BankAccount account = bankAccountRepository.findByIdAndUser(accountId, user)
                        .orElseThrow(() -> new RuntimeException("Account not found or access denied"));
                patterns = recurringPatternRepository.findByBankAccount_Id(accountId);
            } else {
                patterns = recurringPatternRepository.findByBankAccount_User_Id(userId);
            }
            
            List<RecurringResponseDto> responses = patterns.stream()
                    .map(recurringMapper::toDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", responses.size(),
                    "recurring", responses
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 GET: Retrieve Summary (from insights table)
    // ---------------------------------------------------------------------
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long accountId) {
        try {
            Long userId = extractUserIdFromJwt(authHeader);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            InsightSummaryResponse summary;
            if (accountId != null) {
                BankAccount account = bankAccountRepository.findByIdAndUser(accountId, user)
                        .orElseThrow(() -> new RuntimeException("Account not found or access denied"));
                summary = insightService.getLatestSummaryByAccount(accountId);
            } else {
                summary = insightService.getLatestSummaryByUser(userId);
            }
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "summary", summary
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private AnomalyResponse toAnomalyResponse(AnomalyRecord anomaly) {
        AnomalyResponse response = new AnomalyResponse();
        response.setId(anomaly.getId());
        response.setTransactionId(anomaly.getTransaction().getId());
        response.setMerchant(anomaly.getTransaction().getMerchant());
        response.setAmount(anomaly.getTransaction().getAmount());
        response.setTransactionDate(anomaly.getTransaction().getDate());
        response.setReason(anomaly.getReason());
        response.setAnomalyType(anomaly.getAnomalyType());
        response.setConfidence(anomaly.getConfidence());
        response.setCreatedAt(anomaly.getCreatedAt());
        return response;
    }
}