// HdfcStatementService (original restored)
package com.Life_ledger.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Life_ledger.Enum.CategorySource;
import com.Life_ledger.Enum.TransactionEnum;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.util.TransactionFingerprintUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HdfcStatementService {

    private final PDFReaderService pdfReader;
    private final FileExtractorService fileExtractor;
    private final HdfcStatementParser hdfcParser;
    private final BankAccountRepository bankRepo;
    private final TransactionRepository transactionRepo;
    private final TransactionFingerprintUtil fingerprintUtil;
    private final RuleBasedCategoryService ruleBasedCategoryService;

    public Map<String, Object> parseFile(MultipartFile file, User user) {

        Map<String, Object> response = new HashMap<>();

        String name = file.getOriginalFilename().toLowerCase();
        boolean isCsv = name.endsWith(".csv");

        try {
            String extractedText;

            if (name.endsWith(".pdf")) {
                try {
                    extractedText = pdfReader.extractText(file);
                } catch (IllegalArgumentException e) {
                    return Map.of(
                            "status", "error",
                            "message", "PDF is password protected. Please upload an unprotected file.");
                }
            } else if (isCsv) {
                extractedText = fileExtractor.extractCsv(file);
            } else {
                return Map.of(
                        "status", "error",
                        "message", "Unsupported file type");
            }
            Map<String, Object> parsed = hdfcParser.parse(extractedText, isCsv);
            String accountNumber = parsed.get("accountNumber").toString();
            List<Map<String, Object>> txns = (List<Map<String, Object>>) parsed.get("transactions");

            if (accountNumber.equals("UNKNOWN")) {
                return Map.of(
                        "status", "error",
                        "message", "Unable to extract account number from the statement");
            }

            BankAccount bankAccount = bankRepo
                    .findFirstByEncryptedAccountNumberAndUserId(accountNumber, user.getId())
                    .orElse(null);

            boolean newAccount = false;

            if (bankAccount == null) {
                bankAccount = BankAccount.builder()
                        .encryptedAccountNumber(accountNumber)
                        .last4Digits(accountNumber.substring(accountNumber.length() - 4))
                        .bankName("HDFC")
                        .user(user)
                        .build();

                bankRepo.save(bankAccount);
                newAccount = true;

                System.out.println("New account detected! Added: " + accountNumber);
            } else {
                System.out.println("Existing account linked: " + accountNumber);
            }

            int savedCount = 0;

            List<Map<String, Object>> added = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();

            for (Map<String, Object> t : txns) {
                try {
                    String reference = (String) t.getOrDefault("reference", "");
                    if (reference == null || reference.isBlank()) {
                        reference = "AUTO-" + UUID.randomUUID();
                    }

                    String rawDescription = (String) t.getOrDefault("description", "");

                    // 1️⃣ Parse date & amount first
                    LocalDate date = LocalDate.parse(t.get("date").toString());
                    BigDecimal amount = new BigDecimal(t.get("amount").toString());

                    // 2️⃣ Decide transaction type BEFORE we use it
                    String typeStr = t.getOrDefault("type", "DEBIT").toString().toUpperCase();
                    TransactionEnum txnType = typeStr.equals("CREDIT")
                            ? TransactionEnum.CREDIT
                            : TransactionEnum.DEBIT;

                    String normalizedDesc = rawDescription.trim().toLowerCase().replaceAll("\\s+", " ");

                    // -----------------------------------------------------
                    // CREATE FINGERPRINT (UNIQUE TRANSACTION ID)
                    // -----------------------------------------------------
                    String fingerprint = t.get("date") + "|"
                            + t.get("amount") + "|"
                            + t.get("type") + "|"
                            + normalizedDesc + "|"
                            + reference + "|"
                            + bankAccount.getId();

                    // -----------------------------------------------------
                    // DUPLICATE CHECK
                    // -----------------------------------------------------
                    boolean exists = transactionRepo.existsByFingerprintAndBankAccountId(fingerprint,
                            bankAccount.getId());

                    // 3️⃣ Build fingerprint using shared util
                    // String fingerprint = t.get("date") + "|"
                    // + t.get("amount") + "|"
                    // + t.get("type") + "|"
                    // + normalizedDesc + "|"
                    // + reference + "|"
                    // + bankAccount.getId();

                    // boolean exists = transactionRepo.existsByFingerprintAndBankAccountId(
                    // fingerprint,
                    // bankAccount.getId());

                    if (exists) {
                        t.put("reason", "duplicate_fingerprint");
                        skipped.add(t);
                        continue;
                    }

                    // 4️⃣ Build & save transaction
                    Optional<Category> categoryOpt = ruleBasedCategoryService.categorize(rawDescription, user);

                    Transaction txn = Transaction.builder()
                            .merchant(rawDescription)
                            .reference(reference)
                            .fingerprint(fingerprint)
                            .amount(amount)
                            .typeTransaction(txnType)
                            .date(date)
                            .notes(rawDescription)
                            .category(categoryOpt.orElse(null))
                            .categorySource(
                                    categoryOpt.isPresent()
                                            ? CategorySource.RULE
                                            : CategorySource.UNSET)
                            .anomaly(false)
                            .recurring(false)
                            .bankAccount(bankAccount)
                            .build();

                    transactionRepo.save(txn);
                    savedCount++;
                    added.add(t);

                } catch (Exception ex) {
                    t.put("reason", "parse_error");
                    t.put("errorMessage", ex.getMessage());
                    skipped.add(t);

                    System.out.println("⚠ Failed to save transaction: " + ex.getMessage());
                }
            }

            response.put("status", "success");
            response.put("message", "File parsed successfully");
            response.put("accountNumber", accountNumber);
            response.put("newAccount", newAccount);
            response.put("transactionsImported", savedCount);
            response.put("added", added);
            response.put("skipped", skipped);

            return response;

        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Parsing failed: " + e.getMessage());
        }
    }
}
