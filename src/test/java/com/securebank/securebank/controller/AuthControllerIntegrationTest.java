package com.securebank.securebank.controller;

import com.securebank.securebank.BaseIntegrationTest;
import com.securebank.securebank.dto.request.LoginRequest;
import com.securebank.securebank.dto.request.RegisterRequest;
import com.securebank.securebank.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithTokens() {
        String id = uid();
        AuthResponse body = webClient.post()
                .uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("user" + id, id + "@test.com", DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.getAccessToken()).isNotBlank();
        assertThat(body.getRefreshToken()).isNotBlank();
        assertThat(body.getTokenType()).isEqualTo("Bearer");
        assertThat(body.getExpiresIn()).isPositive();
        assertThat(body.getRole()).isEqualTo("USER");
    }

    @Test
    void register_duplicateUsername_returns409() {
        String id = uid();
        webClient.post().uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("user" + id, id + "@test.com", DEFAULT_PASSWORD))
                .exchange().expectStatus().isCreated();

        webClient.post().uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("user" + id, "other_" + id + "@test.com", DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void register_duplicateEmail_returns409() {
        String id = uid();
        webClient.post().uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("user" + id, id + "@test.com", DEFAULT_PASSWORD))
                .exchange().expectStatus().isCreated();

        webClient.post().uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("other" + id, id + "@test.com", DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void register_weakPassword_returns400() {
        webClient.post().uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("user" + uid(), uid() + "@test.com", "weakpass"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_invalidEmail_returns400() {
        webClient.post().uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("user" + uid(), "not-an-email", DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithTokens() {
        String id = uid();
        String username = "user" + id;
        registerUser(username, id + "@test.com");

        AuthResponse body = webClient.post()
                .uri(API + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(username, DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.getAccessToken()).isNotBlank();
        assertThat(body.getUsername()).isEqualTo(username);
    }

    @Test
    void login_wrongPassword_returns401() {
        String id = uid();
        registerUser("user" + id, id + "@test.com");

        webClient.post().uri(API + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("user" + id, "WrongP@ss9"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknownUser_returns401() {
        webClient.post().uri(API + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("ghost" + uid(), DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── Token enforcement ─────────────────────────────────────────────────────

    @Test
    void accessProtectedEndpoint_withoutToken_returns401() {
        getNoAuth("/accounts").expectStatus().isUnauthorized();
    }

    @Test
    void accessProtectedEndpoint_withValidToken_returns200() {
        String id = uid();
        AuthResponse auth = registerUser("user" + id, id + "@test.com");
        get("/accounts", auth.getAccessToken()).expectStatus().isOk();
    }

    @Test
    void accessProtectedEndpoint_withTamperedToken_returns401() {
        String id = uid();
        AuthResponse auth = registerUser("user" + id, id + "@test.com");
        String tampered = auth.getAccessToken() + "tampered";

        webClient.get().uri(API + "/accounts")
                .headers(h -> h.setBearerAuth(tampered))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
