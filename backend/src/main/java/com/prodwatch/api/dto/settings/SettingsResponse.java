package com.prodwatch.api.dto.settings;

import java.math.BigDecimal;
import java.time.Instant;

public record SettingsResponse(
        String businessName,
        String contactEmail,
        BigDecimal taxRate,
        String taxLabel,
        String receiptFooter,
        String receiptLogoUrl,
        String businessMode,
        Instant updatedAt) {}
