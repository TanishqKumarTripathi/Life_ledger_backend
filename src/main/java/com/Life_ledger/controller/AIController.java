package com.Life_ledger.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Life_ledger.dto.ai.AIAnalysisRequest;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.GeminiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
// @Autowired
public class AIController {
    private final GeminiService geminiService;
    private final JwtUtil jwtUtils;
    private final UserRepository userRepository;

    @GetMapping("/test-gemini")
    public ResponseEntity<String> testGemini() {
        return ResponseEntity.ok(geminiService.testModel());
    }

    @GetMapping("/models")
    public ResponseEntity<String> listModels() {
        return ResponseEntity.ok(geminiService.listModels());
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @RequestHeader("Authorization") String tokenHeader) {

        // Strip "Bearer "
        String token = tokenHeader.substring(7);

        // Extract email from token
        String email = jwtUtils.extractUsername(token);

        // Fetch user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        Long userId = user.getId();
        // Call AI service
        Object result = geminiService.analyzeUserTransactions(userId);

        return ResponseEntity.ok(result);
    }

}
