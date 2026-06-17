package com.securebank.securebank.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
        UUID accountId,
        String accountNumber,
        BigDecimal balance,
        String currency
) {}
