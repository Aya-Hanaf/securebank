package com.securebank.securebank.controller;

import com.securebank.securebank.dto.request.ChangePasswordRequest;
import com.securebank.securebank.dto.request.SetUserEnabledRequest;
import com.securebank.securebank.dto.request.UpdateProfileRequest;
import com.securebank.securebank.dto.response.UserResponse;
import com.securebank.securebank.security.UserPrincipal;
import com.securebank.securebank.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and admin management")
public class UserController {

    private final UserService userService;

    // ── Own profile ───────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user's profile")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update authenticated user's username and email")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change authenticated user's password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] List all users (paginated)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Get any user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Enable or disable a user account")
    public ResponseEntity<UserResponse> setUserEnabled(
            @PathVariable UUID id,
            @Valid @RequestBody SetUserEnabledRequest request) {
        return ResponseEntity.ok(userService.setUserEnabled(id, request));
    }
}
