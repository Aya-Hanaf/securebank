package com.securebank.securebank.service;

import com.securebank.securebank.dto.request.CreateAccountRequest;
import com.securebank.securebank.dto.request.UpdateAccountStatusRequest;
import com.securebank.securebank.dto.response.AccountResponse;
import com.securebank.securebank.dto.response.BalanceResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request, UUID userId);

    List<AccountResponse> getMyAccounts(UUID userId);

    AccountResponse getAccountById(UUID accountId, UUID userId);

    BalanceResponse getBalance(UUID accountId, UUID userId);

    AccountResponse updateStatus(UUID accountId, UpdateAccountStatusRequest request, UUID userId);
}
