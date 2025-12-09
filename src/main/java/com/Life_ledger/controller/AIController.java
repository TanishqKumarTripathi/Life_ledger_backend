package com.Life_ledger.controller;

import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.InsightRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.GeminiService;
import com.Life_ledger.service.InsightService;
import com.Life_ledger.service.RecurringPatternService;
import com.fasterxml.jackson.databind.ObjectMapper;

//import com.Life_ledger.service.GeminiServiceImpl.Step;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final GeminiService geminiService;
    private final InsightService insightService;
    private final JwtUtil jwtUtils;
    private final UserRepository userRepository;
    private final InsightRepository insightRepository;
    private final RecurringPatternService recurringPatternService;

    private final ObjectMapper objectMapper = new ObjectMapper();



    // TEST + MODEL LIST (unchanged)
    @GetMapping("/test-gemini")
    public ResponseEntity<String> testGemini() {
        return ResponseEntity.ok(geminiService.testModel());
    }

    @GetMapping("/models")
    public ResponseEntity<String> listModels() {
        return ResponseEntity.ok(geminiService.listModels());
    }


    // RUN AI ANALYSIS → SAVE TO DB (Called when user opens AI Insights)
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestParam(required = false) Long accountId) {

        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            Long userId = user.getId();

            // Run AI analysis - account-specific or user-wide
            Object analysisResult = accountId != null 
                ? geminiService.analyzeAccountTransactions(accountId)
                : geminiService.analyzeUserTransactions(userId);
            
            // Extract just the analysis part for insight storage
            Map<String, Object> result = (Map<String, Object>) analysisResult;
            Object analysis = result.get("analysis");
            String jsonText = objectMapper.writeValueAsString(analysis);

            // Save insight with correct AI text
            Insight insight = new Insight();
            insight.setAiText(jsonText);
            insight.setUser(user);
            
            // Set bank account if analyzing specific account
            if (accountId != null) {
                BankAccount bankAccount = new BankAccount();
                bankAccount.setId(accountId);
                insight.setBankAccount(bankAccount);
            }
            
            insight = insightRepository.save(insight);

            // Extract and save recurring patterns
            try {
                recurringPatternService.processInsightPatterns(insight);
                System.out.println("✅ Recurring patterns processed successfully");
            } catch (Exception patternEx) {
                System.err.println("❌ Pattern processing failed: " + patternEx.getMessage());
                patternEx.printStackTrace();
                // Continue execution even if pattern processing fails
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "AI analysis completed and saved",
                    "analysis", analysisResult
            ));

        } catch (Exception ex) {
            System.err.println("❌ AI Analysis Error: " + ex.getMessage());
            ex.printStackTrace();
            
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage(),
                    "details", ex.getClass().getSimpleName()
            ));
        }
    }


    //GET LATEST SAVED INSIGHT FOR LOGGED-IN USER
    @GetMapping("/latest")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getLatestInsight(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestParam(required = false) Long accountId) {

        try {
            // Extract user
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            // Fetch latest insight - account-specific or user-wide
            Insight insight = accountId != null
                    ? insightRepository.findTopByBankAccountIdOrderByCreatedAtDesc(accountId).orElse(null)
                    : insightRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);

            if (insight == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "empty",
                        "message", "No insights found. Please run analysis first."
                ));
            }

            // Convert entity → DTO (fix for LOB stream error)
            InsightResponseDTO dto = InsightResponseDTO.fromEntity(insight);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "insight", dto
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }

    //GET SPECIFIC INSIGHT SECTION (health, spending, patterns, anomalies)
    @GetMapping("/insights/sections/{sectionName}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getInsightSection(
            @RequestHeader("Authorization") String tokenHeader,
            @PathVariable String sectionName,
            @RequestParam(required = false) Long accountId) {

        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            Object sectionData = accountId != null
                ? insightService.getInsightSectionByAccount(accountId, sectionName)
                : insightService.getInsightSection(user.getId(), sectionName);

            if (sectionData == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "empty",
                        "message", "No data found for section: " + sectionName
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "section", sectionName,
                    "data", sectionData
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }
    // CHECK IF ANALYSIS EXISTS
    @GetMapping("/insights/status")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAnalysisStatus(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestParam(required = false) Long accountId) {

        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            Insight latest = accountId != null
                ? insightService.getLatestInsightByAccount(accountId)
                : insightService.getLatestInsight(user.getId());
            boolean hasRecent = accountId != null
                ? insightService.hasRecentAnalysisByAccount(accountId, 24)
                : insightService.hasRecentAnalysis(user.getId(), 24);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "hasAnalysis", latest != null,
                    "hasRecentAnalysis", hasRecent,
                    "lastAnalysisDate", latest != null ? latest.getCreatedAt() : null
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }
}
