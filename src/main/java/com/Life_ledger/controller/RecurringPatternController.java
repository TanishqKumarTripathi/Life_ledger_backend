package com.Life_ledger.controller;

import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.RecurringPatternService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringPatternController {

    private final RecurringPatternService recurringPatternService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private User getUserFromToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or malformed Authorization header");
        }
        try {
            String token = header.substring(7);
            String email = jwtUtil.extractUsername(token);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (Exception ex) {
            throw new RuntimeException("Invalid or expired token");
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("Authorization") String token,
            @RequestBody RecurringPattern recurringPattern) {
        try {
            getUserFromToken(token);
            return ResponseEntity.ok(recurringPatternService.createRecurringPattern(recurringPattern));
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        try {
            getUserFromToken(token);
            return ResponseEntity.ok(recurringPatternService.getRecurringPattern(id));
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader("Authorization") String token) {
        try {
            getUserFromToken(token);
            return ResponseEntity.ok(recurringPatternService.getAllRecurringPatterns());
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody RecurringPattern recurringPattern) {
        try {
            getUserFromToken(token);
            return ResponseEntity.ok(recurringPatternService.updateRecurringPattern(id, recurringPattern));
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        try {
            getUserFromToken(token);
            recurringPatternService.deleteRecurringPattern(id);
            return ResponseEntity.ok("Recurring Pattern deleted successfully");
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }
}
