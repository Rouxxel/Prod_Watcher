package com.prodwatch.api.dto.movement;

import java.time.Instant;
import java.util.UUID;

import com.prodwatch.api.entity.StockMovementType;

public record StockMovementResponse(
        UUID id,
        StockMovementType type,
        UUID productId,
        String productName,
        int qty,
        UUID fromWarehouseId,
        UUID toWarehouseId,
        String provider,
        String recipient,
        UUID userId,
        String userName,
        Instant timestamp,
        String note) {}
