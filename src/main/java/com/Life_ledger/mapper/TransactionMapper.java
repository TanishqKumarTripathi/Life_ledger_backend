package com.Life_ledger.mapper;

import org.springframework.stereotype.Component;

import com.Life_ledger.dto.transaction.TransactionResponse;
import com.Life_ledger.entity.Transaction;

@Component
public class TransactionMapper {
    public TransactionResponse toResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .merchant(txn.getMerchant())
                .amount(txn.getAmount())
                .date(txn.getDate())
                .notes(txn.getNotes())
                .recurring(txn.isRecurring())
                .anomaly(txn.isAnomaly())
                .bankAccountId(txn.getBankAccount() != null ? txn.getBankAccount().getId() : null)
                .categoryId(txn.getCategory() != null ? txn.getCategory().getId() : null)
                .build();
    }
}
