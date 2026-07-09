package com.prodwatch.api.dto.warehouse;

import jakarta.validation.constraints.Size;

/** PATCH body — {@code null} fields are left unchanged. */
public record WarehouseUpdate(@Size(max = 255) String name, @Size(max = 255) String location) {}
