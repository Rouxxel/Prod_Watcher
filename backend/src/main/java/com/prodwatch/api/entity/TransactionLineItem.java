package com.prodwatch.api.entity;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionLineItem(
        UUID productId, String name, String sku, int qty, BigDecimal unitPrice) {}
