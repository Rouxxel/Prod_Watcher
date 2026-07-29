package com.prodwatch.api.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Product catalog fields cached in Redis — stock is always loaded live from the ledger. */
public record ProductMeta(
        UUID id,
        String name,
        String sku,
        String category,
        BigDecimal price,
        UUID defaultWarehouseId,
        int lowStockThreshold,
        List<String> images) {}
