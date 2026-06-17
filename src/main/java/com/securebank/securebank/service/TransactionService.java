package com.securebank.securebank.service;

import com.securebank.securebank.dto.request.DepositRequest;
import com.securebank.securebank.dto.request.TransferRequest;
import com.securebank.securebank.dto.request.WithdrawRequest;
import com.securebank.securebank.dto.response.TransactionResponse;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse deposit(DepositRequest request, UUID userId);

    TransactionResponse withdraw(WithdrawRequest request, UUID userId);

    TransactionResponse transfer(TransferRequest request, UUID userId);

    TransactionResponse getTransactionById(UUID transactionId, UUID userId);

    List<TransactionResponse> getAccountTransactions(UUID accountId, UUID userId);

    byte[] generateReceipt(UUID transactionId, UUID userId);
}
