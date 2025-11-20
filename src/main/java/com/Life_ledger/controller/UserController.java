package com.Life_ledger.controller;

import com.Life_ledger.dto.auth.LoginRequest;
import com.Life_ledger.dto.auth.SignupRequest;
import com.Life_ledger.dto.user.UserResponse;
import com.Life_ledger.entity.User;
import com.Life_ledger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Get user profile
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    // Upload/Update profile pic
    @PostMapping("/{id}/profile-pic")
    public ResponseEntity<String> uploadProfilePic(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String url = userService.uploadProfilePic(id, file);
        return ResponseEntity.ok(url);
    }

    // Update user info
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserResponse userRequest) {
        UserResponse updated = userService.updateUser(id, userRequest);
        return ResponseEntity.ok(updated);
    }
}
