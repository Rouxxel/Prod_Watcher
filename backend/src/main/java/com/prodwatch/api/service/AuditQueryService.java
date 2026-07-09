package com.prodwatch.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.audit.AuditEntryResponse;
import com.prodwatch.api.entity.AuditEntry;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.AuditEntryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final AuditEntryRepository auditEntryRepository;

    public AuditQueryService(AuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditEntryResponse> list(String entity, UUID userId, Instant from, Instant to) {
        return auditEntryRepository.findWithFilters(entity, userId, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditEntryResponse get(UUID id) {
        return toResponse(load(id));
    }

    private AuditEntry load(UUID id) {
        return auditEntryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Audit entry not found"));
    }

    private AuditEntryResponse toResponse(AuditEntry entry) {
        return new AuditEntryResponse(
                entry.getId(),
                entry.getUser().getId(),
                entry.getAction(),
                entry.getEntity(),
                entry.getEntityId(),
                entry.getCreatedAt(),
                entry.getDetails());
    }
}
