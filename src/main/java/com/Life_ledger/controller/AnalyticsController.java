package com.Life_ledger.controller;

import com.Life_ledger.dto.analytic.AnalyticsDTO;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.AnalyticsService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    // --------------------------------------------------------
    //   Extract user from JWT token
    // --------------------------------------------------------
    private User getUserFromToken(String header) {
        if (header == null || !header.startsWith("Bearer "))
            throw new RuntimeException("Missing or malformed Authorization header");

        String token = header.substring(7);
        String email = jwtUtil.extractUsername(token);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestAnalytics(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long accountId) {

        try {
            User user = getUserFromToken(token);
            AnalyticsDTO analytics;

            // ----------------------------------------------
            // If accountId provided → fetch analytics for that one account
            // ----------------------------------------------
            if (accountId != null) {

                BankAccount account = bankAccountRepository.findByIdAndUser(accountId, user)
                        .orElseThrow(() -> new RuntimeException("Account not found or access denied"));

                analytics = analyticsService.getLatestAnalyticsByAccount(account.getId());
            }

            else {
                List<BankAccount> accounts = bankAccountRepository.findByUserId(user.getId());
                if (accounts.isEmpty())
                    throw new RuntimeException("No bank accounts found");

                analytics = analyticsService.getLatestAnalytics(user.getId());
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "analytics", analytics
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }
}
