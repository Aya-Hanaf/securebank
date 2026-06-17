package com.securebank.securebank.controller;

import com.securebank.securebank.BaseIntegrationTest;
import com.securebank.securebank.dto.request.DepositRequest;
import com.securebank.securebank.dto.request.TransferRequest;
import com.securebank.securebank.dto.request.WithdrawRequest;
import com.securebank.securebank.dto.response.AccountResponse;
import com.securebank.securebank.dto.response.BalanceResponse;
import com.securebank.securebank.dto.response.TransactionResponse;
import com.securebank.securebank.entity.AccountType;
import com.securebank.securebank.entity.TransactionStatus;
import com.securebank.securebank.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionControllerIntegrationTest extends BaseIntegrationTest {

    private String token1;
    private AccountResponse account1;

    private String token2;
    private AccountResponse account2;

    @BeforeEach
    void setUp() {
        String id = uid();
        token1   = registerUser("user1_" + id, "u1_" + id + "@test.com").getAccessToken();
        account1 = createAccount(token1, AccountType.CHECKING, "EUR");

        token2   = registerUser("user2_" + id, "u2_" + id + "@test.com").getAccessToken();
        account2 = createAccount(token2, AccountType.CHECKING, "EUR");
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    @Test
    void deposit_toOwnActiveAccount_returns201AndCreditsBalance() {
        TransactionResponse tx = post("/transactions/deposit",
                new DepositRequest(account1.getId(), new BigDecimal("500.00"), "salary"), token1)
                .expectStatus().isCreated()
                .expectBody(TransactionResponse.class)
                .returnResult().getResponseBody();

        assertThat(tx.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(tx.getAmount()).isEqualByComparingTo("500.00");
        assertThat(tx.getReference()).startsWith("TXN");
        assertThat(balance(account1, token1)).isEqualByComparingTo("500.00");
    }

    @Test
    void deposit_zeroAmount_returns400() {
        post("/transactions/deposit",
                new DepositRequest(account1.getId(), BigDecimal.ZERO, null), token1)
                .expectStatus().isBadRequest();
    }

    @Test
    void deposit_toAnotherUsersAccount_returns404() {
        post("/transactions/deposit",
                new DepositRequest(account2.getId(), new BigDecimal("100.00"), null), token1)
                .expectStatus().isNotFound();
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    @Test
    void withdraw_sufficientFunds_returns201AndDebitsBalance() {
        deposit(account1.getId(), new BigDecimal("1000.00"), token1);

        TransactionResponse tx = post("/transactions/withdraw",
                new WithdrawRequest(account1.getId(), new BigDecimal("350.00"), "rent"), token1)
                .expectStatus().isCreated()
                .expectBody(TransactionResponse.class)
                .returnResult().getResponseBody();

        assertThat(tx.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(balance(account1, token1)).isEqualByComparingTo("650.00");
    }

    @Test
    void withdraw_insufficientFunds_returns422() {
        deposit(account1.getId(), new BigDecimal("100.00"), token1);

        post("/transactions/withdraw",
                new WithdrawRequest(account1.getId(), new BigDecimal("500.00"), "overspend"), token1)
                .expectStatus().isEqualTo(422);

        assertThat(balance(account1, token1)).isEqualByComparingTo("100.00");
    }

    @Test
    void withdraw_exactBalance_succeeds() {
        deposit(account1.getId(), new BigDecimal("200.00"), token1);

        post("/transactions/withdraw",
                new WithdrawRequest(account1.getId(), new BigDecimal("200.00"), "exact"), token1)
                .expectStatus().isCreated();

        assertThat(balance(account1, token1)).isEqualByComparingTo("0.00");
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    @Test
    void transfer_toExternalAccount_updatesBalancesAtomically() {
        deposit(account1.getId(), new BigDecimal("1000.00"), token1);

        TransactionResponse tx = post("/transactions/transfer",
                new TransferRequest(account1.getId(), account2.getAccountNumber(),
                        new BigDecimal("300.00"), "payment"), token1)
                .expectStatus().isCreated()
                .expectBody(TransactionResponse.class)
                .returnResult().getResponseBody();

        assertThat(tx.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(tx.getTargetAccountNumber()).isEqualTo(account2.getAccountNumber());
        assertThat(balance(account1, token1)).isEqualByComparingTo("700.00");
        assertThat(balance(account2, token2)).isEqualByComparingTo("300.00");
    }

    @Test
    void transfer_betweenOwnAccounts_succeeds() {
        AccountResponse second = createAccount(token1, AccountType.SAVINGS, "EUR");
        deposit(account1.getId(), new BigDecimal("500.00"), token1);

        post("/transactions/transfer",
                new TransferRequest(account1.getId(), second.getAccountNumber(),
                        new BigDecimal("200.00"), "savings"), token1)
                .expectStatus().isCreated();

        assertThat(balance(account1, token1)).isEqualByComparingTo("300.00");
        assertThat(balance(second, token1)).isEqualByComparingTo("200.00");
    }

    @Test
    void transfer_toSameAccount_returns400() {
        deposit(account1.getId(), new BigDecimal("500.00"), token1);

        post("/transactions/transfer",
                new TransferRequest(account1.getId(), account1.getAccountNumber(),
                        new BigDecimal("100.00"), "self"), token1)
                .expectStatus().isBadRequest();
    }

    @Test
    void transfer_insufficientFunds_returns422AndRollsBack() {
        deposit(account1.getId(), new BigDecimal("50.00"), token1);

        post("/transactions/transfer",
                new TransferRequest(account1.getId(), account2.getAccountNumber(),
                        new BigDecimal("200.00"), "overspend"), token1)
                .expectStatus().isEqualTo(422);

        assertThat(balance(account1, token1)).isEqualByComparingTo("50.00");
        assertThat(balance(account2, token2)).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_toNonExistentAccount_returns404() {
        deposit(account1.getId(), new BigDecimal("500.00"), token1);

        post("/transactions/transfer",
                new TransferRequest(account1.getId(), "SB00000000000000",
                        new BigDecimal("100.00"), null), token1)
                .expectStatus().isNotFound();
    }

    // ── PDF receipt ───────────────────────────────────────────────────────────

    @Test
    void getReceipt_ownTransaction_returnsPdf() {
        deposit(account1.getId(), new BigDecimal("100.00"), token1);

        UUID txId = get("/accounts/" + account1.getId() + "/transactions", token1)
                .expectStatus().isOk()
                .expectBodyList(TransactionResponse.class)
                .returnResult().getResponseBody()
                .get(0).getId();

        byte[] pdf = webClient.get()
                .uri(API + "/transactions/" + txId + "/receipt")
                .headers(h -> h.setBearerAuth(token1))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectBody(byte[].class)
                .returnResult().getResponseBody();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    // ── History ───────────────────────────────────────────────────────────────

    @Test
    void getAccountTransactions_returnsAllNewestFirst() {
        deposit(account1.getId(), new BigDecimal("100.00"), token1);
        deposit(account1.getId(), new BigDecimal("200.00"), token1);
        post("/transactions/withdraw",
                new WithdrawRequest(account1.getId(), new BigDecimal("50.00"), "atm"), token1)
                .expectStatus().isCreated();

        List<TransactionResponse> txList = get(
                "/accounts/" + account1.getId() + "/transactions", token1)
                .expectStatus().isOk()
                .expectBodyList(TransactionResponse.class)
                .returnResult().getResponseBody();

        assertThat(txList).hasSize(3);
        for (int i = 0; i < txList.size() - 1; i++) {
            assertThat(txList.get(i).getCreatedAt())
                    .isAfterOrEqualTo(txList.get(i + 1).getCreatedAt());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal balance(AccountResponse account, String token) {
        return get("/accounts/" + account.getId() + "/balance", token)
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .returnResult().getResponseBody()
                .balance();
    }
}
