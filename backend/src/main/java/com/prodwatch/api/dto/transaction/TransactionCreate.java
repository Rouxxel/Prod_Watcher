package com.prodwatch.api.dto.transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TransactionCreate(
        @NotEmpty List<@Valid CartItemDto> items,
        @NotNull @DecimalMin("0.00") BigDecimal subtotal,
        @NotNull @DecimalMin("0.00") BigDecimal tax,
        @NotNull @DecimalMin("0.00") BigDecimal total) {}
