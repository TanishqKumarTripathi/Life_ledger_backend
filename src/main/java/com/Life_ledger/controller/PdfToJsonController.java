package com.Life_ledger.controller;

import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.HdfcStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfToJsonController {

    private final HdfcStatementService hdfcStatementService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // ---------------------------
    // Extract USER from JWT token
    // ---------------------------
    private User getUserFromToken(String header) {
        try {
            if (header == null || !header.startsWith("Bearer "))
                throw new RuntimeException("Missing or invalid Authorization header");

            String token = header.substring(7);
            String email = jwtUtil.extractUsername(token);

            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token");
        }
    }

    // ===========================
    // UPLOAD FILE
    // ===========================
    @PostMapping("/upload")
    public ResponseEntity<?> uploadHdfcStatement(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file) {

        try {
            User user = getUserFromToken(token);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No file uploaded"));
            }

            Map<String, Object> result = hdfcStatementService.parseFile(file, user);

            return ResponseEntity.ok(result);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Server error: " + ex.getMessage()));
        }
    }
}