package com.Life_ledger.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Life_ledger.dto.recurring.RecurringResponseDto;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.Recurringmapper;
import com.Life_ledger.repository.RecurringPatternRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.RecurringPatternService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringPatternController {

    private final RecurringPatternRepository recurringPatternRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final Recurringmapper recurringMapper;
    private final RecurringPatternService recurringPatternService;

    private User getUserFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    /**
     * Get all recurring transactions for a bank account
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<RecurringResponseDto>> getRecurringByAccount(
            @RequestHeader("Authorization") String token,
            @PathVariable Long accountId) {

        User user = getUserFromToken(token);
        List<RecurringPattern> patterns = recurringPatternRepository.findByBankAccount_Id(accountId);

        List<RecurringResponseDto> response = patterns.stream()
                .map(recurringMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all recurring transactions for the authenticated user
     */
    @GetMapping("/user")
    public ResponseEntity<List<RecurringResponseDto>> getRecurringByUser(
            @RequestHeader("Authorization") String token) {

        User user = getUserFromToken(token);
        List<RecurringPattern> patterns = recurringPatternRepository.findByBankAccount_User_Id(user.getId());

        List<RecurringResponseDto> response = patterns.stream()
                .map(recurringMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<RecurringResponseDto> addRecurringPattern(
            @RequestHeader("Authorization") String token,
            @RequestBody RecurringPattern request) {

        User user = getUserFromToken(token);

        RecurringPattern saved = recurringPatternService.createRecurringPattern(request, user.getId());
        return ResponseEntity.ok(recurringMapper.toDto(saved));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<RecurringResponseDto> updateRecurringPattern(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody RecurringPattern request) {

        User user = getUserFromToken(token);

        RecurringPattern updated = recurringPatternService.updateRecurringPattern(id, request, user.getId());
        return ResponseEntity.ok(recurringMapper.toDto(updated));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteRecurringPattern(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        User user = getUserFromToken(token);

        recurringPatternService.deleteRecurringPattern(id, user.getId());

        return ResponseEntity.ok("Recurring Pattern deleted successfully");
    }

}
