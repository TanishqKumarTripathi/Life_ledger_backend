package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.AnalyticsDTO;
import com.Life_ledger.dto.analytic.DashboardResponseDTO;

public interface AnalyticsService {
    AnalyticsDTO getLatestAnalytics(Long userId);
    // DashboardStatsDto getDashboardStats(Long userId);

    // List<CategoryInsightDto> getCategorySpending(Long userId, int days);

    // List<CategoryInsightDto> getCategorySpending(Long userId, int days, Long
    // accountId);

    // List<MonthlyInsightDto> getMonthlySpending(Long userId, int months);

    DashboardResponseDTO getDashboard(Long userId, Long accountId);
}
