package com.Life_ledger.service;

import com.Life_ledger.dto.auth.LoginRequest;
import com.Life_ledger.dto.auth.SignupRequest;
import com.Life_ledger.dto.user.UserInfoDto;
import com.Life_ledger.dto.user.UserResponse;
import com.Life_ledger.dto.auth.AuthResponse;
import com.Life_ledger.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getUserById(Long userId);

    String uploadProfilePic(Long userId, MultipartFile file);

    UserResponse updateUser(Long userId, UserResponse user);

    User getUserByEmail(String email);

    UserInfoDto getFullUserInfo(Long userId);
}
