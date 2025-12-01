package com.Life_ledger.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // MAIN ENTRY POINT
    public Map<String, Object> parseFile(MultipartFile file, User user) {

        Map<String, Object> response = new HashMap<>();

        String name = file.getOriginalFilename().toLowerCase();
        boolean isCsv = name.endsWith(".csv");

        try {

            // -----------------------------------------------------
            // 1️⃣ Detect File Type
            // -----------------------------------------------------
            String extractedText;

            if (name.endsWith(".pdf")) {

                try {
                    extractedText = pdfReader.extractText(file);
                } catch (IllegalArgumentException e) {
                    return Map.of(
                            "status", "error",
                            "message", "PDF is password protected. Upload an unprotected file.");
                }

            } else if (isCsv) {
                extractedText = fileExtractor.extractCsv(file);
            } else {
                return Map.of(
                        "status", "error",
                        "message", "Unsupported file type");
            }

            // -----------------------------------------------------
            // 2️⃣ Extract Data from HDFC Parser
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
                // CREATE NEW ACCOUNT
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
            // 4️⃣ SAVE TRANSACTIONS
            // -----------------------------------------------------
            int savedCount = 0;

            for (Map<String, Object> t : txns) {
                try {
                    String typeStr = t.getOrDefault("type", "DEBIT").toString().toUpperCase();
                    TransactionEnum txnType = typeStr.equals("CREDIT")
                            ? TransactionEnum.CREDIT
                            : TransactionEnum.DEBIT;

                    Transaction txn = Transaction.builder()
                            .merchant((String) t.getOrDefault("description", ""))
                            .reference((String) t.getOrDefault("reference", "")) // <-- FIX
                            .amount(new BigDecimal(t.get("amount").toString()))
                            .typeTransaction(txnType) // <-- FIX
                            .date(LocalDate.parse(t.get("date").toString()))
                            .notes((String) t.getOrDefault("description", "")) // merchant or description
                            .anomaly(false)
                            .recurring(false)
                            .bankAccount(bankAccount) // <-- FIX
                            .build();

                    transactionRepo.save(txn);
                    savedCount++;

                } catch (Exception ex) {
                    System.out.println("⚠ Failed to save a transaction: " + ex.getMessage());
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

            return response;

        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Parsing failed: " + e.getMessage());
        }
    }
}
