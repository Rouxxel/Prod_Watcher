package com.prodwatch.api.dto.settings;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH body — {@code null} fields are left unchanged. */
public record SettingsUpdateRequest(
        @Size(max = 255) String businessName,
        @Email @Size(max = 255) String contactEmail,
        @DecimalMin("0") @DecimalMax("1") BigDecimal taxRate,
        @Size(max = 64) String taxLabel,
        @Size(max = 500) String receiptFooter,
        @Pattern(regexp = "^(https?://).*$", message = "must be an http or https URL")
                @Size(max = 2048)
                String receiptLogoUrl,
        @Pattern(regexp = "auto|single|multi", message = "must be auto, single, or multi") String businessMode) {}
