package com.Life_ledger.controller;

import com.Life_ledger.entity.AnomalyRecord;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.AnomalyRecordService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyRecordController {

    private final AnomalyRecordService anomalyService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // -------------------------------
    // Helper: Extract user from JWT
    // -------------------------------
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

    // -------------------------------
    // CREATE anomaly record (AI saves)
    // -------------------------------
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("Authorization") String token,
            @RequestBody AnomalyRecord anomalyRequest) {

        try {
            User user = getUser(token);

            anomalyRequest.setUser(user);

            AnomalyRecord saved = anomalyService.saveAnomaly(anomalyRequest);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Anomaly record saved",
                    "anomaly", saved));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    // -------------------------------
    // GET single anomaly (owner only)
    // -------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        try {
            User user = getUser(token);
            AnomalyRecord anomaly = anomalyService.getAnomaly(id);

            if (!anomaly.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "error",
                        "message", "Access denied"));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "anomaly", anomaly));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    // -------------------------------
    // GET all anomalies for logged-in user
    // -------------------------------
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestHeader("Authorization") String token) {

        try {
            User user = getUser(token);

            List<AnomalyRecord> anomalies = anomalyService.getAnomaliesByUser(user.getId());

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

    // -------------------------------
    // MARK anomaly resolved
    // -------------------------------
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        try {
            User user = getUser(token);
            AnomalyRecord anomaly = anomalyService.getAnomaly(id);

            if (!anomaly.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "error",
                        "message", "Access denied"));
            }

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

    // -------------------------------
    // DELETE anomaly
    // -------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        try {
            User user = getUser(token);
            AnomalyRecord anomaly = anomalyService.getAnomaly(id);

            if (!anomaly.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "error",
                        "message", "Access denied"));
            }

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
}
