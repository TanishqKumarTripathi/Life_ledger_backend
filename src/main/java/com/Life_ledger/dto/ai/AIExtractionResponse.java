package com.Life_ledger.dto.ai;

import com.Life_ledger.entity.FileImport;
import com.Life_ledger.entity.Transaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIExtractionResponse {
    private List<AITransaction> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AITransaction {
        private LocalDate date;
        private String merchant;
        private BigDecimal amount;
        private String category;
        private String notes;
    }

    // ⭐ THIS FIXES your "toEntityList" error
    public List<Transaction> toEntityList(FileImport fileImport) {
        List<Transaction> list = new ArrayList<>();

        for (AITransaction tx : transactions) {
            Transaction t = new Transaction();
            t.setDate(tx.getDate());
            t.setMerchant(tx.getMerchant());
            t.setAmount(tx.getAmount());
            t.setNotes(tx.getNotes());
            t.setCategory(null); // AI category mapping later
            t.setAnomaly(false);
            t.setRecurring(false);
            t.setFileImport(fileImport);

            list.add(t);
        }

        return list;
    }
}
