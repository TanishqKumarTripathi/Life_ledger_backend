package com.Life_ledger.controller;

import com.Life_ledger.entity.Nudge;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.NudgeRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nudges")
@RequiredArgsConstructor
public class NudgeController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final NudgeRepository nudgeRepository;

    /**
     * ✅ Get all nudges for logged-in user
     */
    @GetMapping
    public ResponseEntity<List<Nudge>> getUserNudges(
            @RequestHeader("Authorization") String token) {

        token = token.substring(7);

        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        List<Nudge> nudges = nudgeRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());

        return ResponseEntity.ok(nudges);
    }

    /**
     * ✅ Mark a nudge as read
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
}
