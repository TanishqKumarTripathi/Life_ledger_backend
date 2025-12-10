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

    /**
     * ✅ Get ALL recurring patterns of logged-in user
     */
    @GetMapping
    public ResponseEntity<List<RecurringResponseDto>> getMyRecurring(
            @RequestHeader("Authorization") String token) {

        token = token.substring(7);

        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        List<RecurringPattern> patterns = recurringPatternRepository.findByBankAccount_User_Id(user.getId());

        List<RecurringResponseDto> response = patterns.stream()
                .map(recurringMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Get recurring patterns for a specific account
     * ✅ Account ownership validated
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<RecurringResponseDto>> getRecurringByAccount(
            @RequestHeader("Authorization") String token,
            @PathVariable Long accountId) {

        token = token.substring(7);

        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        List<RecurringPattern> patterns = recurringPatternRepository.findByBankAccount_Id(accountId);

        List<RecurringResponseDto> response = patterns.stream()
                .map(recurringMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
