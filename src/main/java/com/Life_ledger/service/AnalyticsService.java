package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.AnalyticsDTO;
import com.Life_ledger.dto.analytic.DashboardResponseDTO;
import com.Life_ledger.dto.analytic.DashboardStatsDto;
import com.Life_ledger.dto.analytic.CategoryInsightDto;
import com.Life_ledger.dto.analytic.MonthlyInsightDto;
import java.util.List;

public interface AnalyticsService {
    AnalyticsDTO getLatestAnalytics(Long userId);
    AnalyticsDTO getLatestAnalyticsByAccount(Long accountId);
    DashboardStatsDto getDashboardStats(Long userId);
    DashboardStatsDto getDashboardStatsByAccount(Long accountId);
    List<CategoryInsightDto> getCategorySpending(Long userId, int days, Long accountId);
    List<MonthlyInsightDto> getMonthlySpending(Long userId, int months);
    List<MonthlyInsightDto> getMonthlySpendingByAccount(Long accountId, int months);
    DashboardResponseDTO getDashboard(Long userId, Long accountId);
}
