package com.prodwatch.api.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseCreate(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String location) {}
