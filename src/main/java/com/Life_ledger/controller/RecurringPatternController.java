package com.Life_ledger.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Life_ledger.dto.recurring.RecurringResponseDto;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.Recurringmapper;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.RecurringPatternRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringPatternController {

    private final RecurringPatternRepository recurringPatternRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final Recurringmapper recurringMapper;

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
}
