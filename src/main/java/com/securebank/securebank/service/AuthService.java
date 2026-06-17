package com.securebank.securebank.service;

import com.securebank.securebank.dto.request.LoginRequest;
import com.securebank.securebank.dto.request.RegisterRequest;
import com.securebank.securebank.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
