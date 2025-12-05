package com.Life_ledger.service;

import com.Life_ledger.dto.analytics.CategoryAnalyticsResponse;
import com.Life_ledger.entity.Transaction;
import com.Life_ledger.Enum.TransactionEnum;
import com.Life_ledger.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public List<CategoryAnalyticsResponse> getCategoryDistribution(Long userId) {

        List<Transaction> expenses =
                transactionRepository.findByUserAndType(userId, TransactionEnum.DEBIT);

        Map<String, BigDecimal> categoryTotals =
                expenses.stream()
                        .filter(t -> t.getCategory() != null)
                        .collect(Collectors.groupingBy(
                                t -> t.getCategory().getName(),
                                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                        ));

        return categoryTotals.entrySet().stream()
                .map(e -> new CategoryAnalyticsResponse(
                        e.getKey(),
                        e.getValue().doubleValue()
                ))
                .collect(Collectors.toList());
    }
}

