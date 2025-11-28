package com.Life_ledger.service;

import com.Life_ledger.dto.auth.LoginRequest;
import com.Life_ledger.dto.auth.SignupRequest;
import com.Life_ledger.dto.category.CategoryResponse;
import com.Life_ledger.dto.goals.GoalResponse;
import com.Life_ledger.dto.transaction.TransactionResponse;
import com.Life_ledger.dto.user.UserInfoDto;
import com.Life_ledger.dto.user.UserResponse;
import com.Life_ledger.dto.account.AccountResponse;
import com.Life_ledger.dto.auth.AuthResponse;
import com.Life_ledger.entity.User;
import com.Life_ledger.mapper.AccountMapper;
import com.Life_ledger.mapper.CategoryMapper;
import com.Life_ledger.mapper.GoalMapper;
import com.Life_ledger.mapper.TransactionMapper;
import com.Life_ledger.mapper.UserMapper;
import com.Life_ledger.repository.BankAccountRepository;
import com.Life_ledger.repository.CategoryRepository;
import com.Life_ledger.repository.GoalRepository;
import com.Life_ledger.repository.TransactionRepository;
import com.Life_ledger.repository.UserRepository;
import com.Life_ledger.security.JwtUtil;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Cloudinary cloudinary;
    private final BankAccountRepository bankAccountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final GoalRepository goalRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .build();

        User savedUser = userRepository.save(user);
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email/password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email/password");
        }

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .message("Login successful") // added
                .build();
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserMapper.toResponse(user);
    }

    @Override
    public String uploadProfilePic(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String url = (String) uploadResult.get("secure_url");

            user.setProfilePicUrl(url);
            userRepository.save(user);

            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile picture", e);
        }
    }

    @Override
    public UserResponse updateUser(Long userId, UserResponse user) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getName() != null)
            existingUser.setName(user.getName());
        if (user.getPhoneNumber() != null)
            existingUser.setPhoneNumber(user.getPhoneNumber());
        if (user.getProfilePicUrl() != null)
            existingUser.setProfilePicUrl(user.getProfilePicUrl());

        User updatedUser = userRepository.save(existingUser);
        return UserMapper.toResponse(updatedUser);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public UserInfoDto getFullUserInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid user"));

        List<AccountResponse> accounts = bankAccountRepository.findAllByUserId(userId)
                .stream()
                .map(AccountMapper::toResponse)
                .collect(Collectors.toList());

        List<CategoryResponse> categories = categoryRepository.findAllByUser_Id(userId)
                .stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());

        // TransactionMapper transactionMapper = new TransactionMapper();
        // List<TransactionResponse> transactions =
        // transactionRepository.findAllByBankAccount_User_Id(userId)
        // .stream()
        // .map(transactionMapper::toResponse)
        // .collect(Collectors.toList());

        List<GoalResponse> goals = goalRepository.findByUser(user)
                .stream()
                .map(GoalMapper::toDto)
                .collect(Collectors.toList());

        return UserInfoDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .accounts(accounts)
                .categories(categories)
                // .transactions(transactions)
                .goals(goals)
                .build();
    }
}
