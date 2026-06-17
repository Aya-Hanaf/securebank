package com.securebank.securebank.dto.request;

import com.securebank.securebank.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(

        @NotNull(message = "Status is required")
        AccountStatus status
) {}
