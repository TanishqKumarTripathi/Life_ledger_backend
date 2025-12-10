package com.Life_ledger.service.ocr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.Life_ledger.Enum.CategorySource;
import com.Life_ledger.dto.OCR.OcrUploadResponseDto;
import com.Life_ledger.dto.OCR.ParsedTransactionDto;
import com.Life_ledger.dto.gemini.GeminiOcrResponseDto;
import com.Life_ledger.dto.gemini.GeminiTransactionDto;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.GeminiOcrMapper;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.service.RuleBasedCategoryService;
import com.Life_ledger.util.TransactionFingerprintUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OcrService {

    // you can keep your old fields here if you still use OCR.space
    private final TransactionRepository transactionRepo;
    private final BankAccountRepository accountRepo;
    private final GeminiOcrMapper geminiOcrMapper;
    private final TransactionFingerprintUtil fingerprintUtil;
    private final ObjectMapper mapper;
    private final RuleBasedCategoryService ruleBasedCategoryService;

    /**
     * NEW: process Gemini JSON text and save to DB.
     */
    public OcrUploadResponseDto processGeminiText(
            String geminiJson,
            Long accountId,
            User user) {

        BankAccount account = accountRepo
                .findByIdAndUser(accountId, user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        try {
            String cleanJson = extractJson(geminiJson);

            GeminiOcrResponseDto response = mapper.readValue(cleanJson, GeminiOcrResponseDto.class);

            if (response.getTransactions() == null || response.getTransactions().isEmpty()) {
                throw new RuntimeException("No transactions detected");
            }

            List<Transaction> entitiesToSave = new ArrayList<>();
            List<ParsedTransactionDto> added = new ArrayList<>();
            List<ParsedTransactionDto> skipped = new ArrayList<>();

            for (GeminiTransactionDto tx : response.getTransactions()) {

                String reference = tx.getReference();
                if (reference == null || reference.isBlank()) {
                    reference = "GEMINI";
                }

                String rawDescription = tx.getDescription();

                String cleanDescription = tx.getDescription()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9]", "")
                        .trim();

                String fingerprint = fingerprintUtil.build(
                        tx.getDate(),
                        tx.getAmount(),
                        tx.getType(),
                        cleanDescription,
                        account.getId());

                boolean exists = transactionRepo
                        .existsByFingerprintAndBankAccountId(fingerprint, account.getId());

                ParsedTransactionDto dto = ParsedTransactionDto.builder()
                        .date(tx.getDate())
                        .description(tx.getDescription())
                        .amount(tx.getAmount())
                        .type(tx.getType())
                        .build();

                if (exists) {
                    skipped.add(dto);
                    continue;
                }

                Optional<Category> categoryOpt = ruleBasedCategoryService.categorize(rawDescription, user);

                Transaction entity = geminiOcrMapper.toEntityWithCategory(
                        tx,
                        account,
                        fingerprint,
                        reference,
                        categoryOpt.orElse(null),
                        categoryOpt.isPresent()
                                ? CategorySource.RULE
                                : CategorySource.UNSET);

                entitiesToSave.add(entity);
                added.add(dto);
            }

            // ✅ batch save
            if (!entitiesToSave.isEmpty()) {
                transactionRepo.saveAll(entitiesToSave);
            }

            return OcrUploadResponseDto.builder()
                    .success(true)
                    .message("Image processed successfully")
                    .transactionsImported(added.size())
                    .transactionsSkipped(skipped.size())
                    .added(added)
                    .skipped(skipped)
                    .accountId(accountId)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse Gemini OCR JSON", e);
        }
    }

    private String extractJson(String text) {
        if (text == null)
            return "{}";

        text = text.trim();

        // Remove ```json or ```
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*", "").trim();
            text = text.replaceAll("```$", "").trim();
        }

        // Extract the first JSON object or array
        int objStart = text.indexOf("{");
        int arrStart = text.indexOf("[");

        int start = -1;
        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            start = objStart;
        } else if (arrStart >= 0) {
            start = arrStart;
        }

        if (start == -1)
            return "{}";

        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';

        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open)
                depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        return text.substring(start);
    }

}
