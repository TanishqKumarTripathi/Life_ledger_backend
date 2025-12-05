package com.Life_ledger.controller;

import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.RecurringPatternRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring/fix")
@RequiredArgsConstructor
public class RecurringPatternFixController {

    private final RecurringPatternRepository recurringPatternRepository;
    private final TransactionRepository transactionRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/bank-accounts")
    @Transactional
    public ResponseEntity<?> fixBankAccounts(@RequestHeader("Authorization") String tokenHeader) {
        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtil.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<RecurringPattern> patterns = recurringPatternRepository.findByUserIdWithBankAccount(user.getId());
            int fixed = 0;

            for (RecurringPattern pattern : patterns) {
                if (pattern.getBankAccount() == null && pattern.getMerchant() != null) {
                    List<Transaction> matchingTxns = transactionRepository.findByMerchantAndAmountRange(
                        user.getId(), pattern.getMerchant(), pattern.getAmount(), new BigDecimal("50.00")
                    );
                    
                    if (!matchingTxns.isEmpty() && matchingTxns.get(0).getBankAccount() != null) {
                        pattern.setBankAccount(matchingTxns.get(0).getBankAccount());
                        pattern.setTransaction(matchingTxns.get(0));
                        recurringPatternRepository.save(pattern);
                        fixed++;
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Fixed " + fixed + " recurring patterns with bank accounts"
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }
}