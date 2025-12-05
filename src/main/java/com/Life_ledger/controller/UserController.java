package com.Life_ledger.controller;

import com.Life_ledger.dto.user.UserResponse;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.Life_ledger.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // Only ONE userService needed — use it for everything
    private final UserService userService;

    // -------------------------
    // 🔒 Extract & Validate User from JWT
    // -------------------------
    private User getUserFromToken(String token) {
        try {
            token = token.substring(7);
            String email = jwtUtil.extractUsername(token);

            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid user"));
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token");
        }
    }

    // ----------------------------------------------------------
    // 1️⃣ FULL USER INFO (ACCOUNTS + TRANSACTIONS + GOALS + CATEGORIES)
    // GET /api/user/info
    // ----------------------------------------------------------
    @GetMapping("/info")
    public ResponseEntity<?> getFullInfo(
            @RequestHeader("Authorization") String token) {
        try {
            User user = getUserFromToken(token);
            return ResponseEntity.ok(userService.getFullUserInfo(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", e.getMessage()));
        }
    }

    // ----------------------------------------------------------
    // 2️⃣ USER PROFILE DATA (Name, Email, Phone, Pic)
    // GET /api/user/profile
    // ----------------------------------------------------------
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
        try {
            User user = getUserFromToken(token);
            UserResponse profile = userService.getUserById(user.getId());
            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to fetch profile", e.getMessage()));
        }
    }

    // ----------------------------------------------------------
    // 3️⃣ UPDATE USER PROFILE
    // PUT /api/user/update
    // ----------------------------------------------------------
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UserResponse request) {

        try {
            User user = getUserFromToken(token);
            UserResponse updated = userService.updateUser(user.getId(), request);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Update failed", e.getMessage()));
        }
    }

    // ----------------------------------------------------------
    // 4️⃣ UPLOAD / UPDATE PROFILE PIC
    // POST /api/user/profile-pic
    // ----------------------------------------------------------
    @PostMapping("/profile-pic")
    public ResponseEntity<?> uploadProfilePic(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file) {

        try {
            User user = getUserFromToken(token);
            String url = userService.uploadProfilePic(user.getId(), file);
            return ResponseEntity.ok(new UploadResponse("Uploaded successfully", url));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Upload failed", e.getMessage()));
        }
    }

    // -------------------------
    // Simple JSON error response
    // -------------------------
    record ErrorResponse(String status, String message) {
    }

    record UploadResponse(String message, String url) {
    }
}
