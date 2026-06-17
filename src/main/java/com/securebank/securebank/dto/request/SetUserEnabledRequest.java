package com.securebank.securebank.dto.request;

import jakarta.validation.constraints.NotNull;

public record SetUserEnabledRequest(@NotNull Boolean enabled) {}
