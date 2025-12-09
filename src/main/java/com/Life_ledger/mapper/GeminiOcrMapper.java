package com.Life_ledger.mapper;

import org.springframework.stereotype.Component;

import com.Life_ledger.Enum.CategorySource;
import com.Life_ledger.dto.gemini.GeminiTransactionDto;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Category;
import com.Life_ledger.entity.Transaction;

@Component
public class GeminiOcrMapper {

    public Transaction toEntity(
            GeminiTransactionDto src,
            BankAccount account,
            String fingerprint,
            String reference) {
        return Transaction.builder()
                .bankAccount(account)
                .date(src.getDate())
                .merchant(src.getDescription())
                .amount(src.getAmount())
                .typeTransaction(src.getType())
                .reference(reference)
                .fingerprint(fingerprint)
                .notes("Imported via Gemini OCR")
                .anomaly(false)
                .recurring(false)
                .build();
    }

    public Transaction toEntityWithCategory(
            GeminiTransactionDto src,
            BankAccount account,
            String fingerprint,
            String reference,
            Category category,
            CategorySource source) {

        return Transaction.builder()
                .bankAccount(account)
                .date(src.getDate())
                .merchant(src.getDescription()) // ✅ RAW
                .notes(src.getDescription())
                .amount(src.getAmount())
                .typeTransaction(src.getType())
                .reference(reference)
                .fingerprint(fingerprint)

                // ✅ category info passed explicitly
                .category(category)
                .categorySource(source)

                .anomaly(false)
                .recurring(false)
                .build();
    }
}
