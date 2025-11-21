package com.Life_ledger.mapper;

import com.Life_ledger.dto.user.UserResponse;
import com.Life_ledger.entity.User;

public class UserMapper {
    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profilePicUrl(user.getProfilePicUrl())
                .build();
    }
}
