package com.prodwatch.api.dto.movement;

import java.util.UUID;

import com.prodwatch.api.entity.StockMovementType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockMovementCreate(
        @NotNull StockMovementType type,
        @NotNull UUID productId,
        @Min(1) int qty,
        UUID fromWarehouseId,
        UUID toWarehouseId,
        String note) {}
