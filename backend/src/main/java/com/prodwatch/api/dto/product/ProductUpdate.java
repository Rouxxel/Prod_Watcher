package com.prodwatch.api.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** PATCH body — {@code null} fields are left unchanged. */
public record ProductUpdate(
        @Size(max = 255) String name,
        @Size(max = 64) String sku,
        @Size(max = 128) String category,
        @DecimalMin("0.00") BigDecimal price,
        UUID warehouseId,
        @Min(0) Integer lowStockThreshold,
        List<@Size(max = 2048) String> images) {}
