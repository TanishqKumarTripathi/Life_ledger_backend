package com.Life_ledger.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.Life_ledger.dto.OCR.ParsedTransactionDto;
import com.Life_ledger.entity.BankAccount;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.Enum.TransactionEnum;

@Component
public class OcrTransactionMapper {

    public Transaction toEntity(
            ParsedTransactionDto dto,
            BankAccount account) {

        Transaction txn = new Transaction();
        txn.setDate(dto.getDate());
        txn.setMerchant(dto.getDescription());
        txn.setAmount(dto.getAmount());
        txn.setTypeTransaction(dto.getType());
        txn.setBankAccount(account);
        txn.setNotes("Imported via Gemini OCR");

        return txn;
    }
}
