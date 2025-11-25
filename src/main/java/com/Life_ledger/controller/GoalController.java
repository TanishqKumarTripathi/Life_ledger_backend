package com.Life_ledger.controller;

import com.Life_ledger.dto.goals.GoalRequest;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.createGoal(request));
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<GoalResponse> getGoal(@PathVariable Long goalId) {
        return ResponseEntity.ok(goalService.getGoalById(goalId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserGoals(@PathVariable Long userId) {
        return ResponseEntity.ok(goalService.getGoalsByUser(userId));
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable Long goalId,
            @RequestBody GoalRequest request
    ) {
        return ResponseEntity.ok(goalService.updateGoal(goalId, request));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.ok(Map.of("message", "Goal deleted"));
    }

    @PostMapping("/{goalId}/contribute")
    public ResponseEntity<?> contribute(
            @PathVariable Long goalId,
            @RequestBody Map<String, Object> body
    ) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        Optional<String> nudge = goalService.addContribution(goalId, amount);
        return ResponseEntity.ok(Map.of("message", "Contribution added", "nudge", nudge.orElse(null)));
    }

    @GetMapping("/{goalId}/nudge")
    public ResponseEntity<?> evaluateNudge(@PathVariable Long goalId) {
        Optional<String> nudge = goalService.evaluateNudge(goalId);
        return ResponseEntity.ok(Map.of("nudge", nudge.orElse(null)));
    }
}
