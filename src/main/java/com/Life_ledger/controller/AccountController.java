package com.Life_ledger.controller;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;
import com.Life_ledger.service.AccountService;

import lombok.RequiredArgsConstructor;

import com.Life_ledger.mapper.AccountMapper;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final JwtUtil jwtUtils;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody AccountRequest request) {

        token = token.substring(7);

        String email = jwtUtils.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        Long userId = user.getId();

        BankAccount saved = accountService.createAccount(userId, request);
        return ResponseEntity.ok(AccountMapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(@RequestParam Long userId) {
        List<AccountResponse> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }
}
