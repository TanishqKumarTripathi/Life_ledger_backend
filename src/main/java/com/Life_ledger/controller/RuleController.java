package com.Life_ledger.controller;

import com.Life_ledger.dto.rules.*;
// import com.Life_ledger.dto.rule.RuleResponse;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.RuleService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private User getUserFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(
            @RequestHeader("Authorization") String token,
            @RequestBody RuleRequest request) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(ruleService.createRule(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<RuleResponse>> getRules(
            @RequestHeader("Authorization") String token) {

        User user = getUserFromToken(token);
        return ResponseEntity.ok(ruleService.getAllRules(user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRule(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        User user = getUserFromToken(token);
        ruleService.deleteRule(user.getId(), id);

        return ResponseEntity.ok(
                java.util.Map.of("status", "success", "message", "Rule deleted"));
    }
}
