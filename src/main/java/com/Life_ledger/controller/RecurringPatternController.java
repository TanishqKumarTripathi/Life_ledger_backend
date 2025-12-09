package com.Life_ledger.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Life_ledger.dto.recurring.RecurringResponseDto;
import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.mapper.Recurringmapper;
import com.Life_ledger.repository.RecurringPatternRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringPatternController {

    private final RecurringPatternRepository recurringPatternRepository;
    private final Recurringmapper recurringMapper;

    /**
     * Get all recurring transactions for a bank account
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<RecurringResponseDto>> getRecurringByAccount(
            @PathVariable Long accountId) {

        List<RecurringPattern> patterns = recurringPatternRepository.findByBankAccount_Id(accountId);

        List<RecurringResponseDto> response = patterns.stream()
                .map(recurringMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * (Optional) Get all recurring transactions for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecurringResponseDto>> getRecurringByUser(
            @PathVariable Long userId) {

        List<RecurringPattern> patterns = recurringPatternRepository.findByBankAccount_User_Id(userId);

        List<RecurringResponseDto> response = patterns.stream()
                .map(recurringMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
