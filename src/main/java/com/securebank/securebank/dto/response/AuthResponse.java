package com.securebank.securebank.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;

    /** Always "Bearer". */
    @Builder.Default
    private final String tokenType = "Bearer";

    /** Access token lifetime in seconds. */
    private final long expiresIn;

    private final String username;
    private final String role;
}
