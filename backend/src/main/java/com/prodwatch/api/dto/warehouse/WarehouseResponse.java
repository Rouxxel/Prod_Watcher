package com.prodwatch.api.dto.warehouse;

import java.util.UUID;

public record WarehouseResponse(UUID id, String name, String location) {}
