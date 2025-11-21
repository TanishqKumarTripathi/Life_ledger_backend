package com.Life_ledger.controller;

import com.Life_ledger.dto.auth.LoginRequest;
import com.Life_ledger.dto.auth.SignupRequest;
import com.Life_ledger.dto.auth.AuthResponse;
import com.Life_ledger.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

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
}
