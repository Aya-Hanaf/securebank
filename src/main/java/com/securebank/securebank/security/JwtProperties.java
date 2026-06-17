package com.securebank.securebank.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from the {@code jwt.*} block in application.yml.
 * The secret must be a Base64-encoded key of at least 256 bits for HS256.
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(

        @NotBlank
        String secret,

        @Positive
        long expirationMs,

        @Positive
        long refreshExpirationMs
) {}
