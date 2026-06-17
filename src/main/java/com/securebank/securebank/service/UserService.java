package com.securebank.securebank.service;

import com.securebank.securebank.dto.request.ChangePasswordRequest;
import com.securebank.securebank.dto.request.SetUserEnabledRequest;
import com.securebank.securebank.dto.request.UpdateProfileRequest;
import com.securebank.securebank.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getProfile(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(UUID userId);

    UserResponse setUserEnabled(UUID userId, SetUserEnabledRequest request);
}
