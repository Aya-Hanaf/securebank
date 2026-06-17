package com.securebank.securebank.controller;

import com.securebank.securebank.BaseIntegrationTest;
import com.securebank.securebank.dto.request.UpdateAccountStatusRequest;
import com.securebank.securebank.dto.response.AccountResponse;
import com.securebank.securebank.dto.response.BalanceResponse;
import com.securebank.securebank.entity.AccountStatus;
import com.securebank.securebank.entity.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountControllerIntegrationTest extends BaseIntegrationTest {

    private String token;
    private String token2;

    @BeforeEach
    void setUpUsers() {
        String id = uid();
        token  = registerUser("user1_" + id, "u1_" + id + "@test.com").getAccessToken();
        token2 = registerUser("user2_" + id, "u2_" + id + "@test.com").getAccessToken();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void createAccount_validRequest_returns201() {
        AccountResponse body = createAccount(token, AccountType.CHECKING, "EUR");

        assertThat(body.getId()).isNotNull();
        assertThat(body.getAccountNumber()).startsWith("SB");
        assertThat(body.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(body.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(body.getCurrency()).isEqualTo("EUR");
        assertThat(body.getType()).isEqualTo(AccountType.CHECKING);
    }

    @Test
    void createAccount_unauthenticated_returns401() {
        getNoAuth("/accounts").expectStatus().isUnauthorized();
    }

    @Test
    void createAccount_missingType_returns400() {
        post("/accounts", java.util.Map.of("currency", "EUR"), token)
                .expectStatus().isBadRequest();
    }

    @Test
    void createAccount_invalidCurrencyFormat_returns400() {
        post("/accounts", java.util.Map.of("type", "SAVINGS", "currency", "eu"), token)
                .expectStatus().isBadRequest();
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Test
    void getMyAccounts_noAccounts_returnsEmptyList() {
        List<AccountResponse> body = get("/accounts", token)
                .expectStatus().isOk()
                .expectBodyList(AccountResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isEmpty();
    }

    @Test
    void getMyAccounts_withAccounts_returnsOnlyOwnAccounts() {
        createAccount(token,  AccountType.CHECKING, "EUR");
        createAccount(token,  AccountType.SAVINGS,  "USD");
        createAccount(token2, AccountType.CHECKING, "EUR");

        List<AccountResponse> body = get("/accounts", token)
                .expectStatus().isOk()
                .expectBodyList(AccountResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).hasSize(2);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Test
    void getAccountById_ownAccount_returns200() {
        AccountResponse created = createAccount(token, AccountType.SAVINGS, "EUR");

        AccountResponse body = get("/accounts/" + created.getId(), token)
                .expectStatus().isOk()
                .expectBody(AccountResponse.class)
                .returnResult().getResponseBody();

        assertThat(body.getId()).isEqualTo(created.getId());
    }

    @Test
    void getAccountById_otherUsersAccount_returns404() {
        AccountResponse other = createAccount(token2, AccountType.SAVINGS, "EUR");

        get("/accounts/" + other.getId(), token)
                .expectStatus().isNotFound();
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    @Test
    void getBalance_newAccount_returnsZero() {
        AccountResponse account = createAccount(token, AccountType.CHECKING, "EUR");

        BalanceResponse balance = get("/accounts/" + account.getId() + "/balance", token)
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .returnResult().getResponseBody();

        assertThat(balance.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.currency()).isEqualTo("EUR");
    }

    @Test
    void getBalance_afterDeposit_reflectsNewBalance() {
        AccountResponse account = createAccount(token, AccountType.CHECKING, "EUR");
        deposit(account.getId(), new BigDecimal("750.00"), token);

        BalanceResponse balance = get("/accounts/" + account.getId() + "/balance", token)
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .returnResult().getResponseBody();

        assertThat(balance.balance()).isEqualByComparingTo("750.00");
    }

    // ── Status ────────────────────────────────────────────────────────────────

    @Test
    void updateStatus_toInactive_returns200() {
        AccountResponse account = createAccount(token, AccountType.CHECKING, "EUR");

        AccountResponse updated = patch(
                "/accounts/" + account.getId() + "/status",
                new UpdateAccountStatusRequest(AccountStatus.INACTIVE), token)
                .expectStatus().isOk()
                .expectBody(AccountResponse.class)
                .returnResult().getResponseBody();

        assertThat(updated.getStatus()).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void updateStatus_toFrozen_asRegularUser_returns403() {
        AccountResponse account = createAccount(token, AccountType.CHECKING, "EUR");

        patch("/accounts/" + account.getId() + "/status",
                new UpdateAccountStatusRequest(AccountStatus.FROZEN), token)
                .expectStatus().isForbidden();
    }

    // ── Transactions sub-resource ─────────────────────────────────────────────

    @Test
    void getAccountTransactions_returnsHistory() {
        AccountResponse account = createAccount(token, AccountType.CHECKING, "EUR");
        deposit(account.getId(), new BigDecimal("100.00"), token);
        deposit(account.getId(), new BigDecimal("200.00"), token);

        get("/accounts/" + account.getId() + "/transactions", token)
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(2);
    }
}
