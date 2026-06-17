package com.securebank.securebank.service.impl;

import com.securebank.securebank.dto.request.CreateAccountRequest;
import com.securebank.securebank.dto.request.UpdateAccountStatusRequest;
import com.securebank.securebank.dto.response.AccountResponse;
import com.securebank.securebank.dto.response.BalanceResponse;
import com.securebank.securebank.entity.Account;
import com.securebank.securebank.entity.AccountStatus;
import com.securebank.securebank.entity.User;
import com.securebank.securebank.exception.AppException;
import com.securebank.securebank.mapper.AccountMapper;
import com.securebank.securebank.repository.AccountRepository;
import com.securebank.securebank.repository.UserRepository;
import com.securebank.securebank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String ACCOUNT_NUMBER_PREFIX = "SB";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .type(request.type())
                .currency(request.currency())
                .user(user)
                .build();

        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(UUID userId) {
        return accountMapper.toResponseList(accountRepository.findByUser_Id(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId, UUID userId) {
        return accountMapper.toResponse(findOwnedAccount(accountId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId, UUID userId) {
        Account account = findOwnedAccount(accountId, userId);
        return new BalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency()
        );
    }

    @Override
    @Transactional
    public AccountResponse updateStatus(UUID accountId, UpdateAccountStatusRequest request, UUID userId) {
        Account account = findOwnedAccount(accountId, userId);
        AccountStatus newStatus = request.status();

        boolean isFreezeOperation =
                newStatus == AccountStatus.FROZEN || account.getStatus() == AccountStatus.FROZEN;

        if (isFreezeOperation && !isAdmin()) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Only administrators can freeze or unfreeze accounts");
        }

        account.setStatus(newStatus);
        return accountMapper.toResponse(accountRepository.save(account));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Fetches an account by ID, verifying it belongs to the given user.
     * Returns 404 whether the account is missing or owned by someone else
     * to avoid disclosing resource existence to unauthorized callers.
     */
    private Account findOwnedAccount(UUID accountId, UUID userId) {
        return accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            long digits = ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L);
            number = ACCOUNT_NUMBER_PREFIX + digits;
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
