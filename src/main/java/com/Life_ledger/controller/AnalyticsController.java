//package com.Life_ledger.controller;
//
//import com.Life_ledger.security.JwtUtil;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestHeader;
//
//'''@GetMapping("/analytics/latest")
//    public ResponseEntity<?> getLatestAnalytics(@RequestHeader("Authorization") String token) {
//        Long userId = JwtUtil.extractUserId(token);
//        return ResponseEntity.ok(analyticsService.getLatestAnalytics(userId));
//    }
//
//}
