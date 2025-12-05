package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.AnalyticsDTO;

public interface AnalyticsService {
    AnalyticsDTO getLatestAnalytics(Long userId);
}