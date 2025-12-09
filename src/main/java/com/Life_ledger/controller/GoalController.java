package com.Life_ledger.controller;

import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @RequestHeader("Authorization") String token,
            @RequestBody GoalRequest request,
            @RequestParam(required = false) Long accountId) {
        Long userId = getUserIdFromToken(token);
        request.setUserId(userId);
        GoalResponse goal = accountId != null 
            ? goalService.createGoalForAccount(request, accountId)
            : goalService.createGoal(request);
        return ResponseEntity.ok(goal);
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getUserGoals(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long accountId) {
        Long userId = getUserIdFromToken(token);
        List<GoalResponse> goals = accountId != null 
            ? goalService.getGoalsByAccount(accountId)
            : goalService.getGoalsByUser(userId);
        return ResponseEntity.ok(goals);
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @RequestHeader("Authorization") String token,
            @PathVariable Long goalId,
            @RequestBody GoalRequest request) {
        Long userId = getUserIdFromToken(token);
        request.setUserId(userId);
        return ResponseEntity.ok(goalService.updateGoal(goalId, request));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Map<String, String>> deleteGoal(
            @RequestHeader("Authorization") String token,
            @PathVariable Long goalId) {
        Long userId = getUserIdFromToken(token);
        goalService.deleteGoal(goalId, userId);
        return ResponseEntity.ok(Map.of("message", "Goal deleted successfully"));
    }

    @PostMapping("/{goalId}/contribute")
    public ResponseEntity<Map<String, Object>> contribute(
            @RequestHeader("Authorization") String token,
            @PathVariable Long goalId,
            @RequestBody Map<String, Object> body) {
        Long userId = getUserIdFromToken(token);
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        Optional<String> nudge = goalService.addContribution(goalId, amount, userId);

        return ResponseEntity.ok(Map.of(
                "message", "Contribution added successfully",
                "amount", amount,
                "nudge", nudge.orElse(null)));
    }

    @GetMapping("/{goalId}/nudge")
    public ResponseEntity<Map<String, Object>> evaluateNudge(
            @RequestHeader("Authorization") String token,
            @PathVariable Long goalId) {
        Long userId = getUserIdFromToken(token);
        Optional<String> nudge = goalService.evaluateNudge(goalId, userId);
        return ResponseEntity.ok(Map.of("nudge", nudge.orElse("Keep going! You're making progress.")));
    }

    private Long getUserIdFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
        return user.getId();
    }
}