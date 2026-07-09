package com.prodwatch.api.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(
        UUID id,
        UUID userId,
        String userName,
        String action,
        String entity,
        UUID entityId,
        String entityLabel,
        Instant timestamp,
        String details) {}
