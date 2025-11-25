package com.Life_ledger.service;

import com.Life_ledger.entity.FileImport;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.repository.FileImportRepository;
import com.Life_ledger.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleExecutorServiceImpl implements RuleExecutorService {

    private final FileImportRepository fileImportRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public List<Transaction> applyRulesForFileImport(Long fileImportId) {
        FileImport fileImport = fileImportRepository.findById(fileImportId)
                .orElseThrow(() -> new RuntimeException("FileImport not found"));
        List<Transaction> txns = transactionRepository.findByFileImportId(fileImportId);
        applyMerchantRule(txns);
        applyAmountRule(txns);
        applyRecurringRule(txns);
        applyAnomalyRule(txns);
        transactionRepository.saveAll(txns);
        return txns;
    }

    private void applyMerchantRule(List<Transaction> txns) {
        txns.forEach(t -> {
            if (t.getMerchant() != null) {
                t.setMerchant(t.getMerchant().trim().toUpperCase());
            }
        });
    }


    private void applyAmountRule(List<Transaction> txns) {
        BigDecimal limit = new BigDecimal("10000");

        txns.forEach(t -> {
            if (t.getAmount() != null &&
                    t.getAmount().compareTo(limit) > 0) {

                t.setAnomaly(true);
            }
        });
    }


    private void applyRecurringRule(List<Transaction> txns) {
        Map<String, Long> count = txns.stream()
                .filter(t -> t.getMerchant() != null && t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getMerchant().toUpperCase() + "-" + t.getAmount(),
                        Collectors.counting()
                ));

        count.forEach((key, occurrences) -> {
            if (occurrences >= 3) {
                txns.forEach(t -> {
                    String k = t.getMerchant().toUpperCase() + "-" + t.getAmount();
                    if (k.equals(key)) {
                        t.setRecurring(true);
                    }
                });
            }
        });
    }

    private void applyAnomalyRule(List<Transaction> txns) {
        BigDecimal highLimit = new BigDecimal("50000");

        txns.forEach(t -> {
            if (t.getAmount() != null &&
                    t.getAmount().compareTo(highLimit) > 0) {

                t.setAnomaly(true);
            }
        });
    }


}
