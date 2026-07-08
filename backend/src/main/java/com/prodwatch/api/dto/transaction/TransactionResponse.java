package com.prodwatch.api.dto.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.prodwatch.api.entity.TransactionStatus;

public record TransactionResponse(
        UUID id,
        List<CartItemDto> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        UUID cashierId,
        TransactionStatus status,
        Instant timestamp) {}
