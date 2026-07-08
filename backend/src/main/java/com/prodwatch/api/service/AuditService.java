package com.prodwatch.api.service;

import java.util.UUID;

import com.prodwatch.api.entity.AuditEntry;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.repository.AuditEntryRepository;
import com.prodwatch.api.repository.ProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEntryRepository auditEntryRepository;
    private final ProfileRepository profileRepository;

    public AuditService(AuditEntryRepository auditEntryRepository, ProfileRepository profileRepository) {
        this.auditEntryRepository = auditEntryRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public void log(UUID userId, String action, String entity, UUID entityId, String details) {
        Profile user = profileRepository.getReferenceById(userId);
        auditEntryRepository.save(AuditEntry.create(user, action, entity, entityId, details));
    }
}
