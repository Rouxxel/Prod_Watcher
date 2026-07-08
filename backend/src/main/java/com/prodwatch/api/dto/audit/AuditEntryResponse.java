package com.prodwatch.api.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(
        UUID id,
        UUID userId,
        String action,
        String entity,
        UUID entityId,
        Instant timestamp,
        String details) {}
