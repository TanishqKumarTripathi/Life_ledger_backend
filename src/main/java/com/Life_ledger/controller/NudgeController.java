package com.Life_ledger.controller;

import com.Life_ledger.dto.nudge.NudgeDto;
import com.Life_ledger.entity.Goal;
import com.Life_ledger.entity.Nudge;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.NudgeMapper;
import com.Life_ledger.repository.GoalRepository;
import com.Life_ledger.repository.NudgeRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.GoalProgressService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nudges")
@RequiredArgsConstructor
public class NudgeController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final NudgeRepository nudgeRepository;
    private final NudgeMapper nudgeMapper;
    private final GoalProgressService goalProgressService;
    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;

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

    /**
     * ✅ Get all nudges (DTO response)
     */
    @GetMapping
    public ResponseEntity<List<NudgeDto>> getUserNudges(
            @RequestHeader("Authorization") String token) {

        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        List<Nudge> nudges = nudgeRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());

        List<NudgeDto> response = nudges.stream()
                .map(nudgeMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Mark nudge as read
     */
    @PatchMapping("/{nudgeId}/read")
    public ResponseEntity<String> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long nudgeId) {

        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        Nudge nudge = nudgeRepository.findById(nudgeId)
                .orElseThrow(() -> new RuntimeException("Nudge not found"));

        if (!nudge.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        nudge.setRead(true);
        nudgeRepository.save(nudge);

        return ResponseEntity.ok("Nudge marked as read");
    }

    @PostMapping("/recalculate")
    public ResponseEntity<List<NudgeDto>> recalculate(
            @RequestHeader("Authorization") String token) {

        User user = getUserFromToken(token);

        List<Goal> goals = goalRepository.findActiveGoalsByUser(user.getId());
        List<Transaction> txns = transactionRepository.findAllByUserId(user.getId());

        List<NudgeDto> nudges = goalProgressService.evaluateGoals(user, goals, txns);

        return ResponseEntity.ok(nudges);
    }

}
