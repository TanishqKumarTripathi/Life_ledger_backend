package com.Life_ledger.service;

import com.Life_ledger.entity.Insight;

import java.util.List;

public interface InsightService {
    Insight createInsight(Insight insight);
    Insight getInsight(Long id);
    List<Insight> getAllInsights();
    Insight updateInsight(Long id, Insight insight);
    void deleteInsight(Long id);
}
