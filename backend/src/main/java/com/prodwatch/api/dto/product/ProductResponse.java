package com.prodwatch.api.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String category,
        BigDecimal price,
        int stock,
        UUID warehouseId,
        int lowStockThreshold,
        List<String> images) {}
