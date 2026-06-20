package com.securebank.securebank.dto.response;

import com.securebank.securebank.entity.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Jacksonized
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
