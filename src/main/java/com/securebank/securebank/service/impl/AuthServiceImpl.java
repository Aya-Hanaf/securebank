package com.securebank.securebank.service.impl;

import com.securebank.securebank.dto.request.LoginRequest;
import com.securebank.securebank.dto.request.RegisterRequest;
import com.securebank.securebank.dto.response.AuthResponse;
import com.securebank.securebank.entity.Role;
import com.securebank.securebank.entity.User;
import com.securebank.securebank.exception.AppException;
import com.securebank.securebank.repository.UserRepository;
import com.securebank.securebank.security.JwtProperties;
import com.securebank.securebank.security.JwtTokenProvider;
import com.securebank.securebank.security.UserPrincipal;
import com.securebank.securebank.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user.getUsername(), user.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String role = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse("USER");

        return buildAuthResponse(principal.getUsername(), role);
    }

    private AuthResponse buildAuthResponse(String username, String role) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(username))
                .refreshToken(jwtTokenProvider.generateRefreshToken(username))
                .expiresIn(jwtProperties.expirationMs() / 1000)
                .username(username)
                .role(role)
                .build();
    }
}
