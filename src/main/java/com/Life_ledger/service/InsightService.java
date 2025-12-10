package com.Life_ledger.service;

import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.entity.Insight;

import java.util.List;

public interface InsightService {

    Insight createInsight(Insight insight);

    Insight getInsight(Long id);

    List<Insight> getInsightsByBankAccountId(Long accountId);

    Insight updateInsight(Long id, String aiText);

    void deleteInsight(Long id);

    InsightResponseDTO getInsightDTO(Long id);

    // 🚀 DTO version — also bank-account–based
    List<InsightResponseDTO> getInsightsByBankAccountIdDTO(Long accountId);

    List<InsightResponseDTO> getInsightsByAccount(Long accountId);
}
