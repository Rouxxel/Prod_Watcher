package com.prodwatch.api.controller.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.audit.AuditEntryResponse;
import com.prodwatch.api.service.AuditQueryService;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "${config.endpoints.audit_endpoint.endpoint_prefix}${config.endpoints.audit_endpoint.endpoint_route}")
@Tag(name = "audit")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @RateLimit("audit_endpoint")
    @GetMapping
    public List<AuditEntryResponse> list(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return auditQueryService.list(entity, userId, from, to);
    }

    @RateLimit("audit_endpoint")
    @GetMapping("/{id}")
    public AuditEntryResponse get(@PathVariable UUID id) {
        return auditQueryService.get(id);
    }
}
