package com.prodwatch.api.dto.transaction;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartItemDto(
        @NotNull UUID productId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 64) String sku,
        @Min(1) int qty,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        UUID warehouseId) {}
