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

        User user = getUserFromToken(token);

        String rawText = pdfReaderService.extractText(file,
                password != null ? password.trim() : null);

        boolean isCsv = file.getOriginalFilename() != null && 
                       file.getOriginalFilename().toLowerCase().endsWith(".csv");
        
        Map<String, Object> pdfData = hdfcStatementParser.parse(rawText, isCsv);
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
        BankAccount account = accountRepository.findByEncryptedAccountNumberAndUserId(
                encryptedAcc, user.getId()).orElseGet(() -> {
                    BankAccount acc = new BankAccount();
                    acc.setUser(user);
                    acc.setEncryptedAccountNumber(encryptedAcc);
                    acc.setLast4Digits(accountNumber.substring(Math.max(0, accountNumber.length() - 4)));
                    return accountRepository.save(acc);
                });

        List<Map<String, Object>> txns = (List<Map<String, Object>>) pdfData.get("transactions");
        if (txns == null) {
            txns = new ArrayList<>();
        }

        Map<String, Object> saveResult = saveTransactions(txns, account);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Uploaded and processed for user " + user.getName(),
                "bankAccountLast4", account.getLast4Digits(),
                "totalTransactions", saveResult.get("total"),
                "saved", saveResult.get("savedCount"),
                "duplicatesSkipped", saveResult.get("skippedCount"),
                "savedTransactions", saveResult.get("savedTransactions"),
                "skippedTransactions", saveResult.get("skippedTransactions")));
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<?> uploadCsv(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam String accountNumber) {

        User user = getUserFromToken(token);

        String encryptedAcc = encryptionUtil.encrypt(accountNumber);
        BankAccount account = accountRepository.findByEncryptedAccountNumberAndUserId(
                encryptedAcc, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        try {
            String raw = fileExtractorService.extractCsv(file);
            List<Map<String, Object>> txns = hdfcStatementParser.parseCsv(raw);

            Map<String, Object> saveResult = saveTransactions(txns, account);

            return ResponseEntity.ok(Map.of(
                    "bank", "HDFC",
                    "totalTransactions", saveResult.get("total"),
                    "saved", saveResult.get("savedCount"),
                    "duplicatesSkipped", saveResult.get("skippedCount"),
                    "savedTransactions", saveResult.get("savedTransactions"),
                    "skippedTransactions", saveResult.get("skippedTransactions")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private User getUserFromToken(String token) {
        if (token == null || token.length() <= 7) {
            throw new RuntimeException("Invalid token");
        }
        token = token.substring(7);
        String email = jwtUtil.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid user"));
    }

    private Map<String, Object> saveTransactions(List<Map<String, Object>> txns, BankAccount account) {
        if (txns == null) {
            return Map.of("savedCount", 0, "skippedCount", 0, "total", 0, 
                         "savedTransactions", new ArrayList<>(), "skippedTransactions", new ArrayList<>());
        }

        int saved = 0;
        int skipped = 0;
        List<Map<String, Object>> savedList = new ArrayList<>();
        List<Map<String, Object>> skippedList = new ArrayList<>();

        for (Map<String, Object> t : txns) {
            if (t == null) continue;

            String ref = (String) t.get("reference");
            if (ref == null || ref.isBlank()) {
                ref = (String) t.get("description");
            }
            if (ref == null) {
                ref = "UNKNOWN_" + System.currentTimeMillis();
            }

            boolean exists = transactionRepository
                    .findByReferenceAndBankAccountId(ref, account.getId())
                    .isPresent();

            if (exists) {
                skipped++;
                skippedList.add(Map.of(
                    "reference", ref,
                    "merchant", t.get("description"),
                    "amount", t.get("amount"),
                    "date", t.get("date"),
                    "reason", "Duplicate transaction"
                ));
                continue;
            }

            Category category = null;
            SubCategory subCategory = null;

            String description = (String) t.get("description");
            Double amount = (Double) t.get("amount");

            if (description != null && amount != null) {
                Optional<Map<String, Object>> ruleResult = ruleService.applyRules(
                        account.getUser().getId(), description, amount);

                if (ruleResult.isPresent()) {
                    Map<String, Object> r = ruleResult.get();
                    Long categoryId = (Long) r.get("categoryId");
                    Long subCategoryId = (Long) r.get("subCategoryId");

                    if (categoryId != null)
                        category = categoryRepository.findById(categoryId).orElse(null);
                    if (subCategoryId != null)
                        subCategory = subCategoryRepository.findById(subCategoryId).orElse(null);
                }
            }

            String type = (String) t.get("type");
            TransactionEnum txnType = "CREDIT".equalsIgnoreCase(type)
                    ? TransactionEnum.CREDIT
                    : TransactionEnum.DEBIT;

            String dateStr = (String) t.get("date");
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

            Transaction txn = Transaction.builder()
                    .merchant(description != null ? description : "Unknown")
                    .reference(ref)
                    .amount(amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO)
                    .date(date)
                    .typeTransaction(txnType)
                    .bankAccount(account)
                    .category(category)
                    .subCategory(subCategory)
                    .build();

            transactionRepository.save(txn);
            saved++;

            savedList.add(Map.of(
                "reference", ref,
                "merchant", description != null ? description : "Unknown",
                "amount", amount != null ? amount : 0.0,
                "date", dateStr != null ? dateStr : date.toString(),
                "type", type != null ? type : "DEBIT",
                "category", category != null ? category.getName() : null,
                "subCategory", subCategory != null ? subCategory.getName() : null
            ));
        }

        return Map.of(
                "savedCount", saved,
                "skippedCount", skipped,
                "total", txns.size(),
                "savedTransactions", savedList,
                "skippedTransactions", skippedList);
    }
}