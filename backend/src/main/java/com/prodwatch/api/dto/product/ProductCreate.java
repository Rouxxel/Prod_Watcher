package com.prodwatch.api.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreate(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 128) String category,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull UUID warehouseId,
        @Min(0) int lowStockThreshold,
        @NotNull List<@NotBlank String> images) {}
