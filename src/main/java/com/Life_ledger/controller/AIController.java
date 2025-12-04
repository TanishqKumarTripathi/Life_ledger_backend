package com.Life_ledger.controller;

import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.InsightRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.GeminiService;
import com.Life_ledger.service.InsightService;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private final ObjectMapper objectMapper = new ObjectMapper();


    // --------------------------------------------------------------------
    // TEST + MODEL LIST (unchanged)
    // --------------------------------------------------------------------
    @GetMapping("/test-gemini")
    public ResponseEntity<String> testGemini() {
        return ResponseEntity.ok(geminiService.testModel());
    }

    @GetMapping("/models")
    public ResponseEntity<String> listModels() {
        return ResponseEntity.ok(geminiService.listModels());
    }


    // --------------------------------------------------------------------
    // 1️⃣ RUN AI ANALYSIS → SAVE TO DB (Called when user opens AI Insights)
    // --------------------------------------------------------------------
    @PostMapping("/analyze")
    @Transactional
    public ResponseEntity<?> analyze(
            @RequestHeader("Authorization") String tokenHeader) {

        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            Long userId = user.getId();

            // Check if recent analysis exists (within 24 hours)
            if (insightService.hasRecentAnalysis(userId, 24)) {
                return ResponseEntity.ok(Map.of(
                        "status", "recent",
                        "message", "Recent analysis found. Use /latest endpoint to fetch data."
                ));
            }

            // Run Gemini AI analysis
            Object analysisResult = geminiService.analyzeUserTransactions(userId);
            String jsonText = objectMapper.writeValueAsString(analysisResult);

            // Save insight
            Insight insight = new Insight();
            insight.setAiText(jsonText);
            insight.setUser(user);
            insightRepository.save(insight);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "AI analysis completed and saved",
                    "analysis", analysisResult
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }


    // --------------------------------------------------------------------
    // 2️⃣ GET LATEST SAVED INSIGHT FOR LOGGED-IN USER
    // --------------------------------------------------------------------
    @GetMapping("/latest")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getLatestInsight(
            @RequestHeader("Authorization") String tokenHeader) {

        try {
            // Extract user
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            // Fetch latest insight
            Insight insight = insightRepository
                    .findTopByUserIdOrderByCreatedAtDesc(user.getId())
                    .orElse(null);

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

    // --------------------------------------------------------------------
    // 3️⃣ GET SPECIFIC INSIGHT SECTION (health, spending, patterns, anomalies)
    // --------------------------------------------------------------------
    @GetMapping("/insights/sections/{sectionName}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getInsightSection(
            @RequestHeader("Authorization") String tokenHeader,
            @PathVariable String sectionName) {

        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            Object sectionData = insightService.getInsightSection(user.getId(), sectionName);
            
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

    // --------------------------------------------------------------------
    // 4️⃣ CHECK IF ANALYSIS EXISTS
    // --------------------------------------------------------------------
    @GetMapping("/insights/status")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAnalysisStatus(
            @RequestHeader("Authorization") String tokenHeader) {

        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));

            Insight latest = insightService.getLatestInsight(user.getId());
            boolean hasRecent = insightService.hasRecentAnalysis(user.getId(), 24);

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
