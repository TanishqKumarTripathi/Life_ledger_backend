package com.Life_ledger.controller;

import com.Life_ledger.dto.analytic.DashboardResponseDTO;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticController {

    private final AnalyticsService analyticsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // ----------------------------
    // DASHBOARD ENDPOINT
    // ----------------------------
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDTO> getDashboard(
            @RequestParam Long accountId,
            @RequestHeader("Authorization") String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token");
        }

        String token = header.substring(7);
        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DashboardResponseDTO dto = analyticsService.getDashboard(user.getId(), accountId);

        return ResponseEntity.ok(dto);
    }
}
