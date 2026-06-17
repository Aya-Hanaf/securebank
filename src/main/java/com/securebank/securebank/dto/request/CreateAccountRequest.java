package com.securebank.securebank.dto.request;

import com.securebank.securebank.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(

        @NotNull(message = "Account type is required")
        AccountType type,

        @NotNull(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO 4217 code (e.g. EUR, USD)")
        String currency
) {}
