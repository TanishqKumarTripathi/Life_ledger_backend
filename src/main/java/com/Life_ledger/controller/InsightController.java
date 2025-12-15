package com.Life_ledger.controller;

import com.Life_ledger.dto.insight.InsightRequestDTO;
import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Insight;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.InsightService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    // ----------------------------------------------------
    // Extract user from token
    // ----------------------------------------------------
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
            @RequestBody InsightRequestDTO request) {

        try {
            User user = getUserFromToken(token);

            Insight insight = new Insight();
            insight.setAiText(request.getAiText());
            //insight.setUser(user);

            Insight saved = insightService.createInsight(insight);

            InsightResponseDTO dto = InsightResponseDTO.fromEntity(saved);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Insight created successfully",
                    "insight", dto));

        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()));
        }
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<?> getOne(
//            @RequestHeader("Authorization") String token,
//            @PathVariable Long id) {
//
//        try {
//            User user = getUserFromToken(token);
//
//            Insight insight = insightService.getInsight(id);
//
//            if (!insight.getUser().getId().equals(user.getId())) {
//                return ResponseEntity.status(403).body(Map.of(
//                        "status", "error",
//                        "message", "Access denied"));
//            }
//
//            InsightResponseDTO dto = InsightResponseDTO.fromEntity(insight);
//
//            return ResponseEntity.ok(Map.of(
//                    "status", "success",
//                    "insight", dto));
//
//        } catch (Exception ex) {
//            return ResponseEntity.status(400).body(Map.of(
//                    "status", "error",
//                    "message", ex.getMessage()));
//        }
//    }

    // ----------------------------------------------------
    // GET ALL INSIGHTS FOR A SPECIFIC BANK ACCOUNT
    // ----------------------------------------------------
    // @GetMapping("/account/{accountId}")
    // public ResponseEntity<?> getAllByAccount(
    // @RequestHeader("Authorization") String token,
    // @PathVariable Long accountId) {

    // try {
    // User user = getUserFromToken(token);

    // // Validate ownership
    // validateAccountOwner(accountId, user.getId());

    // List<InsightResponseDTO> insights =
    // insightService.getInsightsByBankAccountIdDTO(accountId);

    // return ResponseEntity.ok(Map.of(
    // "status", "success",
    // "count", insights.size(),
    // "insights", insights));

    // } catch (Exception ex) {
    // return ResponseEntity.badRequest().body(Map.of(
    // "status", "error",
    // "message", ex.getMessage()));
    // }
    // }

    // -------------------------------------------------------------------
    // UPDATE INSIGHT
    // -------------------------------------------------------------------
//    @PutMapping("/{id}")
//    public ResponseEntity<?> update(
//            @RequestHeader("Authorization") String token,
//            @PathVariable Long id,
//            @RequestBody InsightRequestDTO request) {
//
//        try {
//            User user = getUserFromToken(token);
//
//            Insight existing = insightService.getInsight(id);
//
//            if (!existing.getUser().getId().equals(user.getId())) {
//                return ResponseEntity.status(403).body(Map.of(
//                        "status", "error",
//                        "message", "Access denied"));
//            }
//
//            Insight updated = insightService.updateInsight(id, request.getAiText());
//            InsightResponseDTO dto = InsightResponseDTO.fromEntity(updated);
//
//            return ResponseEntity.ok(Map.of(
//                    "status", "success",
//                    "message", "Insight updated",
//                    "insight", dto));
//
//        } catch (Exception ex) {
//            return ResponseEntity.status(400).body(Map.of(
//                    "status", "error",
//                    "message", ex.getMessage()));
//        }
//    }

    // -------------------------------------------------------------------
    // DELETE INSIGHT
    // -------------------------------------------------------------------
//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> delete(
//            @RequestHeader("Authorization") String token,
//            @PathVariable Long id) {
//
//        try {
//            User user = getUserFromToken(token);
//
//            Insight existing = insightService.getInsight(id);
//
//            if (!existing.getUser().getId().equals(user.getId())) {
//                return ResponseEntity.status(403).body(Map.of(
//                        "status", "error",
//                        "message", "Access denied"));
//            }
//
//            insightService.deleteInsight(id);
//
//            return ResponseEntity.ok(Map.of(
//                    "status", "success",
//                    "message", "Insight deleted"));
//
//        } catch (Exception ex) {
//            return ResponseEntity.status(400).body(Map.of(
//                    "status", "error",
//                    "message", ex.getMessage()));
//        }
//    }

    //latest
    @GetMapping("/latest")
    public ResponseEntity<?> getLatest(
            @RequestHeader("Authorization") String token) {

        try {
            User user = getUserFromToken(token);

            Insight insight = insightService.getLatestInsight(user.getId());

            if (insight == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "empty",
                        "message", "No insights found. Please run analysis first."
                ));
            }

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

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getInsightsByAccount(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long accountId) {
        // :one: Extract user
        User user = getUserFromToken(authorization);
        // :two: Validate ownership
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", "Access denied"));
        }
        // :three: Call service (transaction is already open there)
        List<InsightResponseDTO> insights = insightService.getInsightsByAccount(accountId);
        // :four: Return clean JSON
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", insights.size(),
                "insights", insights));
    }
    @GetMapping("/account/{accountId}/summary")
    public ResponseEntity<?> getSummaryByAccount(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long accountId) {

        User user = getUserFromToken(authorization);

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", "error",
                    "message", "Access denied"
            ));
        }

        Insight summary = insightService.getLatestSummaryforAccount(accountId);

        if (summary == null) {
            return ResponseEntity.ok(Map.of(
                    "status", "empty",
                    "message", "No summary available. Run analysis first."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "summary", InsightResponseDTO.fromEntity(summary)
        ));
    }



}