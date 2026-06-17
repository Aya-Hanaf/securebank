package com.securebank.securebank.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotNull(message = "Source account ID is required")
        UUID sourceAccountId,

        @NotBlank(message = "Target account number is required")
        String targetAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 15, fraction = 4, message = "Amount exceeds allowed precision (15 integer, 4 decimal)")
        BigDecimal amount,

        @Size(max = 255)
        String description
) {}
