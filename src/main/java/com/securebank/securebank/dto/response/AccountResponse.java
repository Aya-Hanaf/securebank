package com.securebank.securebank.dto.response;

import com.securebank.securebank.entity.AccountStatus;
import com.securebank.securebank.entity.AccountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AccountResponse {

    private UUID id;
    private String accountNumber;
    private AccountType type;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
