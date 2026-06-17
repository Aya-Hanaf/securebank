package com.securebank.securebank.controller;

import com.securebank.securebank.dto.request.CreateAccountRequest;
import com.securebank.securebank.dto.request.UpdateAccountStatusRequest;
import com.securebank.securebank.dto.response.AccountResponse;
import com.securebank.securebank.dto.response.BalanceResponse;
import com.securebank.securebank.dto.response.TransactionResponse;
import com.securebank.securebank.security.UserPrincipal;
import com.securebank.securebank.service.AccountService;
import com.securebank.securebank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    /**
     * POST /api/v1/accounts
     * Opens a new bank account for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request, principal.getId()));
    }

    /**
     * GET /api/v1/accounts
     * Lists all accounts belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.getMyAccounts(principal.getId()));
    }

    /**
     * GET /api/v1/accounts/{id}
     * Returns a single account (must belong to the authenticated user).
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.getAccountById(id, principal.getId()));
    }

    /**
     * GET /api/v1/accounts/{id}/balance
     * Returns the current balance of the account.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.getBalance(id, principal.getId()));
    }

    /**
     * PATCH /api/v1/accounts/{id}/status
     * Changes account status. Freeze/unfreeze operations require ADMIN role.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(accountService.updateStatus(id, request, principal.getId()));
    }

    /**
     * GET /api/v1/accounts/{id}/transactions
     * Lists all transactions for the given account, newest first.
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                transactionService.getAccountTransactions(id, principal.getId()));
    }
}
