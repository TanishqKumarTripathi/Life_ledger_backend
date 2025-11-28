package com.Life_ledger.controller;

import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.SubCategory;
import com.Life_ledger.Enum.TransactionEnum;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.repository.CategoryRepository;
import com.Life_ledger.repository.SubCategoryRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.FileExtractorService;
import com.Life_ledger.service.HdfcStatementParser;
import com.Life_ledger.service.PDFReaderService;
import com.Life_ledger.service.RuleService;
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
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final RuleService ruleService;
    private final JwtUtil jwtUtil;
    private final EncryptionUtil encryptionUtil;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPdf(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam String accountNumber,
            @RequestParam(value = "password", required = false) String password) {

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "No file provided"));
            }

            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Account number required"));
            }

            User user = getUserFromToken(token);
            String rawText = pdfReaderService.extractText(file, password != null ? password.trim() : null);
            
            if (rawText == null || rawText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Could not extract text from file"));
            }

            boolean isCsv = file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".csv");
            Map<String, Object> pdfData = hdfcStatementParser.parse(rawText, isCsv);
            
            if (pdfData == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to parse file"));
            }

            String pdfAccountNumber = (String) pdfData.get("accountNumber");
            if (pdfAccountNumber != null && !"UNKNOWN".equals(pdfAccountNumber)) {
                String cleanUserAcc = accountNumber.replaceAll("\\s+", "");
                String cleanPdfAcc = pdfAccountNumber.replaceAll("\\s+", "");

                if (!cleanUserAcc.equals(cleanPdfAcc)) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "Account mismatch. PDF shows: " + pdfAccountNumber));
                }
            }

            String encryptedAcc = encryptionUtil.encrypt(accountNumber);
            BankAccount account = accountRepository.findFirstByEncryptedAccountNumberAndUserId(encryptedAcc, user.getId())
                    .orElseGet(() -> {
                        BankAccount acc = new BankAccount();
                        acc.setUser(user);
                        acc.setEncryptedAccountNumber(encryptedAcc);
                        acc.setLast4Digits(accountNumber.length() >= 4 ? 
                            accountNumber.substring(accountNumber.length() - 4) : accountNumber);
                        acc.setBankName("HDFC Bank");
                        acc.setAccountName("HDFC Account");
                        return accountRepository.save(acc);
                    });

            Object txnsObj = pdfData.get("transactions");
            List<Map<String, Object>> txns = new ArrayList<>();
            
            if (txnsObj instanceof List) {
                txns = (List<Map<String, Object>>) txnsObj;
            }

            Map<String, Object> saveResult = saveTransactions(txns, account);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Processed successfully",
                    "accountLast4", account.getLast4Digits(),
                    "totalTransactions", saveResult.get("total"),
                    "saved", saveResult.get("savedCount"),
                    "duplicatesSkipped", saveResult.get("skippedCount"),
                    "savedTransactions", saveResult.get("savedTransactions"),
                    "skippedTransactions", saveResult.get("skippedTransactions")));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error", 
                "message", "Processing failed: " + e.getMessage()
            ));
        }
    }

    private User getUserFromToken(String token) {
        if (token == null || token.length() < 8) {
            throw new RuntimeException("Invalid token");
        }
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> saveTransactions(List<Map<String, Object>> txns, BankAccount account) {
        if (txns == null) txns = new ArrayList<>();

        int saved = 0, skipped = 0;
        List<Map<String, Object>> savedList = new ArrayList<>();
        List<Map<String, Object>> skippedList = new ArrayList<>();

        for (Map<String, Object> t : txns) {
            if (t == null) continue;

            try {
                String ref = getStringValue(t, "reference");
                if (ref == null || ref.trim().isEmpty()) {
                    ref = getStringValue(t, "description");
                }
                if (ref == null || ref.trim().isEmpty()) {
                    ref = "UNKNOWN_" + System.currentTimeMillis();
                }

                if (transactionRepository.findByReferenceAndBankAccountId(ref, account.getId()).isPresent()) {
                    skipped++;
                    skippedList.add(Map.of(
                        "reference", ref,
                        "reason", "Duplicate"
                    ));
                    continue;
                }

                String description = getStringValue(t, "description");
                Double amount = getDoubleValue(t, "amount");
                String type = getStringValue(t, "type");
                String dateStr = getStringValue(t, "date");

                if (amount == null) amount = 0.0;
                if (type == null) type = "DEBIT";
                if (description == null) description = "Unknown";

                TransactionEnum txnType = "CREDIT".equalsIgnoreCase(type) ? 
                    TransactionEnum.CREDIT : TransactionEnum.DEBIT;

                LocalDate date;
                try {
                    date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
                } catch (Exception e) {
                    date = LocalDate.now();
                }

                Transaction txn = Transaction.builder()
                        .merchant(description)
                        .reference(ref)
                        .amount(BigDecimal.valueOf(amount))
                        .date(date)
                        .typeTransaction(txnType)
                        .bankAccount(account)
                        .build();

                transactionRepository.save(txn);
                saved++;

                savedList.add(Map.of(
                    "reference", ref,
                    "merchant", description,
                    "amount", amount,
                    "date", date.toString(),
                    "type", txnType.toString()
                ));

            } catch (Exception e) {
                skipped++;
                skippedList.add(Map.of(
                    "reference", "ERROR",
                    "reason", "Processing error: " + e.getMessage()
                ));
            }
        }

        return Map.of(
                "savedCount", saved,
                "skippedCount", skipped,
                "total", txns.size(),
                "savedTransactions", savedList,
                "skippedTransactions", skippedList);
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}