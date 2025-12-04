// HdfcStatementService (original restored)
package com.Life_ledger.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Life_ledger.Enum.TransactionEnum;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HdfcStatementService {

    private final PDFReaderService pdfReader;
    private final FileExtractorService fileExtractor;
    private final HdfcStatementParser hdfcParser;
    private final BankAccountRepository bankRepo;
    private final TransactionRepository transactionRepo;

    // =====================================================================
    // MAIN ENTRY POINT
    // =====================================================================
    public Map<String, Object> parseFile(MultipartFile file, User user) {

        Map<String, Object> response = new HashMap<>();

        String name = file.getOriginalFilename().toLowerCase();
        boolean isCsv = name.endsWith(".csv");

        try {

            // -----------------------------------------------------
            // 1️⃣ Detect File Type + Read Content
            // -----------------------------------------------------
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

            // -----------------------------------------------------
            // 2️⃣ Parse HDFC Statement
            // -----------------------------------------------------
            Map<String, Object> parsed = hdfcParser.parse(extractedText, isCsv);
            String accountNumber = parsed.get("accountNumber").toString();
            List<Map<String, Object>> txns = (List<Map<String, Object>>) parsed.get("transactions");

            if (accountNumber.equals("UNKNOWN")) {
                return Map.of(
                        "status", "error",
                        "message", "Unable to extract account number from the statement");
            }

            // -----------------------------------------------------
            // 3️⃣ BANK ACCOUNT HANDLING
            // -----------------------------------------------------
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

                System.out.println("🔥 New account detected! Added: " + accountNumber);
            } else {
                System.out.println("🔗 Existing account linked: " + accountNumber);
            }

            // -----------------------------------------------------
            // 4️⃣ SAVE TRANSACTIONS (Added + Skipped Tracking)
            // -----------------------------------------------------
            int savedCount = 0;

            List<Map<String, Object>> added = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();

            for (Map<String, Object> t : txns) {
                try {
                    // Normalize reference
                    String reference = (String) t.getOrDefault("reference", "");
                    if (reference == null || reference.isBlank()) {
                        reference = "AUTO-" + UUID.randomUUID();
                    }

                    // Duplicate prevention
                    // -----------------------------------------------------
                    // NORMALIZE DESCRIPTION
                    // -----------------------------------------------------
                    String rawDescription = (String) t.getOrDefault("description", "");
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

                    if (exists) {
                        t.put("reason", "duplicate_fingerprint");
                        skipped.add(t);
                        continue;
                    }

                    // Transaction type
                    String typeStr = t.getOrDefault("type", "DEBIT").toString().toUpperCase();
                    TransactionEnum txnType = typeStr.equals("CREDIT")
                            ? TransactionEnum.CREDIT
                            : TransactionEnum.DEBIT;

                    // Build & save transaction
                    Transaction txn = Transaction.builder()
                            .merchant(rawDescription)
                            .reference(reference)
                            .fingerprint(fingerprint)
                            .amount(new BigDecimal(t.get("amount").toString()))
                            .typeTransaction(txnType)
                            .date(LocalDate.parse(t.get("date").toString()))
                            .notes(rawDescription)
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

            // -----------------------------------------------------
            // 5️⃣ FINAL RESPONSE
            // -----------------------------------------------------
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
