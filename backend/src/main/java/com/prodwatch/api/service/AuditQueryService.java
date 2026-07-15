package com.prodwatch.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.prodwatch.api.dto.audit.AuditEntryResponse;
import com.prodwatch.api.entity.AuditEntry;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.AuditEntryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final AuditEntryRepository auditEntryRepository;
    private final AuditEntityLabelResolver entityLabelResolver;

    public AuditQueryService(
            AuditEntryRepository auditEntryRepository, AuditEntityLabelResolver entityLabelResolver) {
        this.auditEntryRepository = auditEntryRepository;
        this.entityLabelResolver = entityLabelResolver;
    }

    @Transactional(readOnly = true)
    public List<AuditEntryResponse> list(String entity, UUID userId, Instant from, Instant to) {
        List<AuditEntry> entries = auditEntryRepository.findWithFilters(entity, userId, from, to);
        Map<String, String> labels = entityLabelResolver.resolveLabels(entries);
        return entries.stream().map(entry -> toResponse(entry, labels)).toList();
    }

    @Transactional(readOnly = true)
    public AuditEntryResponse get(UUID id) {
        AuditEntry entry = load(id);
        Map<String, String> labels = entityLabelResolver.resolveLabels(List.of(entry));
        return toResponse(entry, labels);
    }

    private AuditEntry load(UUID id) {
        return auditEntryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Audit entry not found"));
    }

    private AuditEntryResponse toResponse(AuditEntry entry, Map<String, String> labels) {
        Profile user = entry.getUser();
        return new AuditEntryResponse(
                entry.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                entry.getAction(),
                entry.getEntity(),
                entry.getEntityId(),
                entityLabelResolver.resolveLabel(entry, labels),
                entry.getCreatedAt(),
                entry.getDetails());
    }
}
