package com.Life_ledger.controller;

import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.Enum.TransactionEnum;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.FileExtractorService;
import com.Life_ledger.service.HdfcStatementParser;
import com.Life_ledger.service.PDFReaderService;
import com.Life_ledger.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfToJsonController {

    private final PDFReaderService pdfReaderService;
    private final HdfcStatementParser hdfcStatementParser;
    private final FileExtractorService fileExtractorService;

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EncryptionUtil encryptionUtil;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPdf(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam String accountNumber,
            @RequestParam(value = "password", required = false) String password) {

        User user = getUserFromToken(token);

        String rawText = pdfReaderService.extractText(file,
                password != null ? password.trim() : null);

        boolean isCsv = file.getOriginalFilename().toLowerCase().endsWith(".csv");

        Map<String, Object> pdfData = hdfcStatementParser.parse(rawText, isCsv);
        String pdfAccountNumber = (String) pdfData.get("accountNumber");

        if (!"UNKNOWN".equals(pdfAccountNumber)) {
            String cleanUserAccount = accountNumber.replaceAll("\\s+", "");
            String cleanPdfAccount = pdfAccountNumber.replaceAll("\\s+", "");
            
            if (!cleanUserAccount.equals(cleanPdfAccount)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Account number mismatch. PDF contains account: " + pdfAccountNumber + 
                              ", but you provided: " + accountNumber
                ));
            }
        }

        String encryptedAccountNumber = encryptionUtil.encrypt(accountNumber);
        BankAccount account = accountRepository.findByEncryptedAccountNumberAndUserId(
                encryptedAccountNumber, user.getId()).orElseGet(() -> {
                    BankAccount newAcc = new BankAccount();
                    newAcc.setUser(user);
                    newAcc.setEncryptedAccountNumber(encryptedAccountNumber);
                    newAcc.setLast4Digits(accountNumber.substring(Math.max(0, accountNumber.length() - 4)));
                    return accountRepository.save(newAcc);
                });

        List<Map<String, Object>> txns = (List<Map<String, Object>>) pdfData.get("transactions");
        saveTransactions(txns, account);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "HDFC statement processed successfully",
                "accountNumber", pdfAccountNumber,
                "transactionsProcessed", txns.size(),
                "data", pdfData));
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<?> uploadCsv(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam String accountNumber) {

        User user = getUserFromToken(token);

        String encryptedAccountNumber = encryptionUtil.encrypt(accountNumber);
        BankAccount account = accountRepository.findByEncryptedAccountNumberAndUserId(
                encryptedAccountNumber, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found for user"));

        try {
            String raw = fileExtractorService.extractCsv(file);
            List<Map<String, Object>> txns = hdfcStatementParser.parseCsv(raw);

            saveTransactions(txns, account);

            Map<String, Object> result = new HashMap<>();
            result.put("bank", "HDFC");
            result.put("transactions", txns);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private User getUserFromToken(String token) {
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    private void saveTransactions(List<Map<String, Object>> txns, BankAccount account) {
        for (Map<String, Object> t : txns) {
            String ref = (String) t.get("reference");
            if (ref == null) ref = (String) t.get("description");

            if (transactionRepository.existsByReference(ref))
                continue;

            String type = (String) t.get("type");
            TransactionEnum transactionType = "CREDIT".equals(type) ? TransactionEnum.CREDIT : TransactionEnum.DEBIT;

            Transaction txn = Transaction.builder()
                    .merchant((String) t.get("description"))
                    .amount(BigDecimal.valueOf((Double) t.get("amount")))
                    .date(LocalDate.parse((String) t.get("date")))
                    .reference(ref)
                    .typeTransaction(transactionType)
                    .bankAccount(account)
                    .build();

            transactionRepository.save(txn);
        }
    }
}