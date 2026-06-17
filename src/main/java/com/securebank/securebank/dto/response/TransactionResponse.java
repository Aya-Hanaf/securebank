package com.securebank.securebank.dto.response;

import com.securebank.securebank.entity.TransactionStatus;
import com.securebank.securebank.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TransactionResponse {

    private UUID id;
    private String reference;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private TransactionStatus status;
    private UUID accountId;
    private String accountNumber;
    private String targetAccountNumber;
    private LocalDateTime createdAt;
}
