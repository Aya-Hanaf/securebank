package com.securebank.securebank.controller;

import com.securebank.securebank.dto.request.DepositRequest;
import com.securebank.securebank.dto.request.TransferRequest;
import com.securebank.securebank.dto.request.WithdrawRequest;
import com.securebank.securebank.dto.response.TransactionResponse;
import com.securebank.securebank.security.UserPrincipal;
import com.securebank.securebank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/v1/transactions/deposit
     * Credits money to the authenticated user's account.
     */
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(request, principal.getId()));
    }

    /**
     * POST /api/v1/transactions/withdraw
     * Debits money from the authenticated user's account.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(request, principal.getId()));
    }

    /**
     * POST /api/v1/transactions/transfer
     * Transfers money from one of the user's accounts to any active account.
     * Both balance updates are committed in a single transaction.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request, principal.getId()));
    }

    /**
     * GET /api/v1/transactions/{id}
     * Returns a single transaction (must belong to the authenticated user).
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                transactionService.getTransactionById(id, principal.getId()));
    }

    /**
     * GET /api/v1/transactions/{id}/receipt
     * Downloads a PDF receipt for the transaction.
     */
    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> getReceipt(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        byte[] pdf = transactionService.generateReceipt(id, principal.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("receipt-" + id + ".pdf")
                .build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
