package com.Life_ledger.service;

import com.Life_ledger.dto.insight.InsightResponseDTO;
import com.Life_ledger.entity.Insight;

import java.util.List;

public interface InsightService {
    Insight createInsight(Insight insight);

    Insight getInsight(Long id);

    List<Insight> getInsightsByUserId(Long userId);

    Insight updateInsight(Long id, String aiText);

    void deleteInsight(Long id);

    List<InsightResponseDTO> getInsightsByUserIdDTO(Long userId);

    InsightResponseDTO getInsightDTO(Long id);

    Insight getLatestInsight(Long userId);
    
    // AI Analysis methods
    Object getInsightSection(Long userId, String sectionName);
    Object getInsightSectionByAccount(Long accountId, String sectionName);
    boolean hasRecentAnalysis(Long userId, int hours);
    boolean hasRecentAnalysisByAccount(Long accountId, int hours);
    Insight getLatestInsightByAccount(Long accountId);
}
