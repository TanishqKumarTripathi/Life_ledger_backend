package com.Life_ledger.controller;

import com.Life_ledger.dto.auth.LoginRequest;
import com.Life_ledger.dto.auth.SignupRequest;
import com.Life_ledger.entity.User;
import com.Life_ledger.repository.RefreshTokenRepository;
import com.Life_ledger.dto.auth.AuthResponse;
import com.Life_ledger.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = userService.signup(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        System.out.println(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername(); // extract email
        User user = userService.getUserByEmail(email);

        refreshTokenRepository.deleteByUserId(user.getId());

        return ResponseEntity.ok("Logged out successfully");
    }
}
