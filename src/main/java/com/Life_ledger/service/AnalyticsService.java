package com.Life_ledger.service;

import com.Life_ledger.dto.analytic.CategoryInsightDto;
import com.Life_ledger.dto.analytic.DashboardStatsDto;
import com.Life_ledger.dto.analytic.MonthlyInsightDto;

import java.util.List;

public interface AnalyticsService {
    DashboardStatsDto getDashboardStats(Long userId);

    List<CategoryInsightDto> getCategorySpending(Long userId, int days);

    List<CategoryInsightDto> getCategorySpending(Long userId, int days, Long accountId);

    List<MonthlyInsightDto> getMonthlySpending(Long userId, int months);
}
