//package com.Life_ledger.controller;
//
//import com.Life_ledger.dto.analytic.CategoryInsightDto;
//import com.Life_ledger.dto.analytic.MonthlyInsightDto;
//import com.Life_ledger.dto.analytic.DashboardStatsDto;
//import com.Life_ledger.entity.User;
//import com.Life_ledger.repository.UserRepository;
//import com.Life_ledger.security.JwtUtil;
//import com.Life_ledger.service.AnalyticsService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/insights")
//@RequiredArgsConstructor
//public class AnalyticController {
//
//    private final AnalyticsService analyticsService;
//    private final JwtUtil jwtUtil;
//    private final UserRepository userRepository;
//
//    private User getUserFromToken(String token) {
//        if (token.startsWith("Bearer "))
//            token = token.substring(7);
//        String email = jwtUtil.extractUsername(token);
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Invalid user"));
//    }
//
//    // Dashboard stats
//    @GetMapping("/dashboard")
//    public ResponseEntity<DashboardStatsDto> getDashboardStats(
//            @RequestHeader("Authorization") String token,
//            @RequestParam(required = false) Long accountId) {
//        User user = getUserFromToken(token);
//        DashboardStatsDto stats = accountId != null
//            ? analyticsService.getDashboardStatsByAccount(accountId)
//            : analyticsService.getDashboardStats(user.getId());
//        return ResponseEntity.ok(stats);
//    }
//
//    // Category spending
//    @GetMapping("/category")
//    public ResponseEntity<List<CategoryInsightDto>> getCategorySpending(
//            @RequestHeader("Authorization") String token,
//            @RequestParam(defaultValue = "30") int days,
//            @RequestParam(required = false) Long accountId) {
//
//        User user = getUserFromToken(token);
//        return ResponseEntity.ok(
//                analyticsService.getCategorySpending(user.getId(), days, accountId));
//    }
//
//    // Monthly spending
//    @GetMapping("/monthly")
//    public ResponseEntity<List<MonthlyInsightDto>> getMonthlySpending(
//            @RequestHeader("Authorization") String token,
//            @RequestParam(defaultValue = "6") int months,
//            @RequestParam(required = false) Long accountId) {
//
//        User user = getUserFromToken(token);
//        List<MonthlyInsightDto> insights = accountId != null
//            ? analyticsService.getMonthlySpendingByAccount(accountId, months)
//            : analyticsService.getMonthlySpending(user.getId(), months);
//        return ResponseEntity.ok(insights);
//    }
//}
