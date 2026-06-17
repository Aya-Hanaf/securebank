package com.securebank.securebank;

import com.securebank.securebank.dto.request.CreateAccountRequest;
import com.securebank.securebank.dto.request.DepositRequest;
import com.securebank.securebank.dto.request.LoginRequest;
import com.securebank.securebank.dto.request.RegisterRequest;
import com.securebank.securebank.dto.response.AccountResponse;
import com.securebank.securebank.dto.response.AuthResponse;
import com.securebank.securebank.dto.response.TransactionResponse;
import com.securebank.securebank.entity.AccountType;
import com.securebank.securebank.repository.AccountRepository;
import com.securebank.securebank.repository.TransactionRepository;
import com.securebank.securebank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    protected static final String API = "/api/v1";
    protected static final String DEFAULT_PASSWORD = "SecureP@ss1";

    // Single container shared across all test classes in the same JVM.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected WebTestClient webClient;

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    protected AuthResponse registerUser(String username, String email) {
        AuthResponse body = webClient.post()
                .uri(API + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(username, email, DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    protected AuthResponse loginUser(String username) {
        AuthResponse body = webClient.post()
                .uri(API + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(username, DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    // ── Account helpers ───────────────────────────────────────────────────────

    protected AccountResponse createAccount(String token, AccountType type, String currency) {
        AccountResponse body = post("/accounts",
                new CreateAccountRequest(type, currency), token)
                .expectStatus().isCreated()
                .expectBody(AccountResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    protected void deposit(UUID accountId, BigDecimal amount, String token) {
        post("/transactions/deposit",
                new DepositRequest(accountId, amount, "test setup deposit"), token)
                .expectStatus().isCreated()
                .expectBody(TransactionResponse.class)
                .returnResult();
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    protected WebTestClient.ResponseSpec post(String path, Object body, String token) {
        return webClient.post()
                .uri(API + path)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    protected WebTestClient.ResponseSpec get(String path, String token) {
        return webClient.get()
                .uri(API + path)
                .headers(h -> h.setBearerAuth(token))
                .exchange();
    }

    protected WebTestClient.ResponseSpec getNoAuth(String path) {
        return webClient.get()
                .uri(API + path)
                .exchange();
    }

    protected WebTestClient.ResponseSpec patch(String path, Object body, String token) {
        return webClient.patch()
                .uri(API + path)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    protected Consumer<HttpHeaders> bearer(String token) {
        return h -> h.setBearerAuth(token);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Returns a short unique string usable in usernames / emails. */
    protected String uid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
