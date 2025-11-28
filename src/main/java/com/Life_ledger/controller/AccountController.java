package com.Life_ledger.controller;

import com.Life_ledger.dto.account.AccountRequest;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.User;
import com.Life_ledger.service.AccountService;
import com.Life_ledger.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;

import com.Life_ledger.mapper.AccountMapper;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
        private final AccountService accountService;
        private final JwtUtil jwtUtil;
        private final UserRepository userRepository;
        private final BankAccountRepository bankAccountRepository;
        private final EncryptionUtil encryptionUtil;

        @PostMapping
        public ResponseEntity<AccountResponse> createAccount(
                        @RequestHeader("Authorization") String token,
                        @RequestBody AccountRequest request) {

                token = token.substring(7);

                String email = jwtUtil.extractUsername(token);
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Invalid user"));

                Long userId = user.getId();

                BankAccount saved = accountService.createAccount(userId, request);
                return ResponseEntity.ok(AccountMapper.toResponse(saved));
        }

        @GetMapping
        public ResponseEntity<List<AccountResponse>> getAccounts(
                        @RequestHeader("Authorization") String token) {

                token = token.substring(7);

                String email = jwtUtil.extractUsername(token);
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Invalid user"));

                List<AccountResponse> accounts = accountService.getUserAccounts(user.getId());
                return ResponseEntity.ok(accounts);
        }

        @GetMapping("/{accountId}/full")
        public ResponseEntity<String> getFullAccountNumber(
                        @RequestHeader("Authorization") String token,
                        @PathVariable Long accountId) {

                token = token.substring(7);
                String email = jwtUtil.extractUsername(token);

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Invalid user"));

                BankAccount account = bankAccountRepository.findById(accountId)
                                .orElseThrow(() -> new RuntimeException("Not found"));

                if (!account.getUser().getId().equals(user.getId()))
                        throw new RuntimeException("Unauthorized");

                String decrypted = encryptionUtil.decrypt(account.getEncryptedAccountNumber());
                return ResponseEntity.ok(decrypted);
        }

        @DeleteMapping("/{accountId}")
        public ResponseEntity<String> deleteAccount(
                        @RequestHeader("Authorization") String token,
                        @PathVariable Long accountId) {

                token = token.substring(7);
                Long userId = jwtUtil.extractUserId(token, userRepository);

                bankAccountRepository.findById(accountId)
                                .orElseThrow(() -> new RuntimeException("Account not found"));

                String msg = accountService.deleteAccount(userId, accountId);

                return ResponseEntity.ok(msg);
        }

        @PutMapping("/{accountId}")
        public ResponseEntity<AccountResponse> updateAccount(
                        @RequestHeader("Authorization") String token,
                        @PathVariable Long accountId,
                        @RequestBody AccountRequest request) {

                token = token.substring(7);
                Long userId = jwtUtil.extractUserId(token, userRepository);

                request.setUserId(userId);

                BankAccount updated = accountService.updateAccount(userId, accountId, request);

                return ResponseEntity.ok(AccountMapper.toResponse(updated));
        }
}