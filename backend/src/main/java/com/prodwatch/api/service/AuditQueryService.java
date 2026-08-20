package com.prodwatch.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.prodwatch.api.dto.audit.AuditEntryResponse;
import com.prodwatch.api.cache.RedisCacheKeys;
import com.prodwatch.api.cache.RedisCacheService;
import com.prodwatch.api.cache.RedisCacheTtls;
import com.prodwatch.api.entity.AuditEntry;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.AuditEntryRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.TenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final AuditEntryRepository auditEntryRepository;
    private final AuditEntityLabelResolver entityLabelResolver;
    private final RedisCacheService cacheService;
    private final RedisCacheTtls cacheTtls;

    public AuditQueryService(
            AuditEntryRepository auditEntryRepository,
            AuditEntityLabelResolver entityLabelResolver,
            RedisCacheService cacheService,
            RedisCacheTtls cacheTtls) {
        this.auditEntryRepository = auditEntryRepository;
        this.entityLabelResolver = entityLabelResolver;
        this.cacheService = cacheService;
        this.cacheTtls = cacheTtls;
    }

    @Transactional(readOnly = true)
    public List<AuditEntryResponse> list(
            String entity, UUID userId, Instant from, Instant to, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        List<AuditEntry> entries = auditEntryRepository.findWithFilters(ecosystemId, entity, userId, from, to);
        Map<String, String> labels = entityLabelResolver.resolveLabels(entries);
        return entries.stream().map(entry -> toResponse(entry, labels)).toList();
    }

    @Transactional(readOnly = true)
    public AuditEntryResponse get(UUID id, CurrentUser user) {
        String cacheKey = RedisCacheKeys.audit(id);
        return cacheService
                .cacheGet(cacheKey, AuditEntryResponse.class)
                .orElseGet(() -> {
                    AuditEntry entry = load(id, user);
                    Map<String, String> labels = entityLabelResolver.resolveLabels(List.of(entry));
                    AuditEntryResponse response = toResponse(entry, labels);
                    cacheService.cacheSet(cacheKey, response, cacheTtls.audit());
                    return response;
                });
    }

    private AuditEntry load(UUID id, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        return auditEntryRepository
                .findByIdAndEcosystem_Id(id, ecosystemId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit entry not found"));
    }

    private AuditEntryResponse toResponse(AuditEntry entry, Map<String, String> labels) {
        Profile user = entry.getUser();
        String entityLabel = entry.getEntityLabel();
        if (entityLabel == null || entityLabel.isBlank()) {
            entityLabel = entityLabelResolver.resolveLabel(entry, labels);
        }
        return new AuditEntryResponse(
                entry.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                entry.getAction(),
                entry.getEntity(),
                entry.getEntityId(),
                entityLabel,
                entry.getCreatedAt(),
                entry.getDetails());
    }
}
