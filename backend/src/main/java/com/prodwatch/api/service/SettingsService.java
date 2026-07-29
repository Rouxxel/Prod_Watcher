package com.prodwatch.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.prodwatch.api.dto.settings.SettingsResponse;
import com.prodwatch.api.dto.settings.SettingsUpdateRequest;
import com.prodwatch.api.cache.RedisCacheKeys;
import com.prodwatch.api.cache.RedisCacheService;
import com.prodwatch.api.cache.RedisCacheTtls;
import com.prodwatch.api.entity.Ecosystem;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.WorkspaceSettings;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.WorkspaceSettingsRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.TenantContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final WorkspaceSettingsRepository workspaceSettingsRepository;
    private final ProfileRepository profileRepository;
    private final AuditService auditService;
    private final EcosystemService ecosystemService;
    private final BigDecimal defaultTaxRate;
    private final RedisCacheService cacheService;
    private final RedisCacheTtls cacheTtls;

    public SettingsService(
            WorkspaceSettingsRepository workspaceSettingsRepository,
            ProfileRepository profileRepository,
            AuditService auditService,
            EcosystemService ecosystemService,
            RedisCacheService cacheService,
            RedisCacheTtls cacheTtls,
            @Value("${prodwatch.pos.tax-rate:0.16}") BigDecimal defaultTaxRate) {
        this.workspaceSettingsRepository = workspaceSettingsRepository;
        this.profileRepository = profileRepository;
        this.auditService = auditService;
        this.ecosystemService = ecosystemService;
        this.cacheService = cacheService;
        this.cacheTtls = cacheTtls;
        this.defaultTaxRate = defaultTaxRate;
    }

    @Transactional
    public SettingsResponse get(CurrentUser user) {
        return getCachedOrLoad(TenantContext.requireEcosystemId(user));
    }

    @Transactional
    public BigDecimal getTaxRate(UUID ecosystemId) {
        String cacheKey = RedisCacheKeys.settings(ecosystemId);
        return cacheService
                .cacheGet(cacheKey, SettingsResponse.class)
                .map(SettingsResponse::taxRate)
                .orElseGet(() -> getOrBootstrap(ecosystemId).getTaxRate());
    }

    @Transactional
    public SettingsResponse update(SettingsUpdateRequest dto, CurrentUser admin) {
        UUID ecosystemId = TenantContext.requireEcosystemId(admin);
        WorkspaceSettings settings = getOrBootstrap(ecosystemId);
        List<String> changes = new ArrayList<>();

        if (dto.businessName() != null && !dto.businessName().equals(settings.getBusinessName())) {
            changes.add("businessName");
            settings.setBusinessName(dto.businessName());
        }
        if (dto.contactEmail() != null && !dto.contactEmail().equals(settings.getContactEmail())) {
            changes.add("contactEmail");
            settings.setContactEmail(dto.contactEmail());
        }
        if (dto.taxRate() != null && dto.taxRate().compareTo(settings.getTaxRate()) != 0) {
            changes.add("taxRate");
            settings.setTaxRate(dto.taxRate());
        }
        if (dto.taxLabel() != null && !dto.taxLabel().equals(settings.getTaxLabel())) {
            changes.add("taxLabel");
            settings.setTaxLabel(dto.taxLabel());
        }
        if (dto.receiptFooter() != null) {
            String footer = blankToNull(dto.receiptFooter());
            if (!Objects.equals(footer, settings.getReceiptFooter())) {
                changes.add("receiptFooter");
                settings.setReceiptFooter(footer);
            }
        }
        if (dto.receiptLogoUrl() != null) {
            String logoUrl = blankToNull(dto.receiptLogoUrl());
            if (!Objects.equals(logoUrl, settings.getReceiptLogoUrl())) {
                changes.add("receiptLogoUrl");
                settings.setReceiptLogoUrl(logoUrl);
            }
        }
        if (dto.businessMode() != null && !dto.businessMode().equals(settings.getBusinessMode())) {
            changes.add("businessMode");
            settings.setBusinessMode(dto.businessMode());
        }

        Profile updater = profileRepository
                .findById(admin.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        settings.setUpdatedBy(updater);
        settings = workspaceSettingsRepository.save(settings);

        if (!changes.isEmpty()) {
            auditService.log(
                    admin.getUserId(),
                    "SETTINGS_UPDATED",
                    "settings",
                    settings.getId(),
                    String.join(", ", changes));
        }

        cacheService.cacheDelete(RedisCacheKeys.settings(ecosystemId));
        return toResponse(settings);
    }

    private SettingsResponse getCachedOrLoad(UUID ecosystemId) {
        String cacheKey = RedisCacheKeys.settings(ecosystemId);
        return cacheService
                .cacheGet(cacheKey, SettingsResponse.class)
                .orElseGet(() -> {
                    SettingsResponse response = toResponse(getOrBootstrap(ecosystemId));
                    cacheService.cacheSet(cacheKey, response, cacheTtls.settings());
                    return response;
                });
    }

    private WorkspaceSettings getOrBootstrap(UUID ecosystemId) {
        return workspaceSettingsRepository
                .findByEcosystem_Id(ecosystemId)
                .orElseGet(() -> {
                    Ecosystem ecosystem = ecosystemService.requireById(ecosystemId);
                    return workspaceSettingsRepository.save(
                            WorkspaceSettings.createDefault(ecosystem, defaultTaxRate));
                });
    }

    private SettingsResponse toResponse(WorkspaceSettings settings) {
        return new SettingsResponse(
                settings.getBusinessName(),
                settings.getContactEmail(),
                settings.getTaxRate(),
                settings.getTaxLabel(),
                settings.getReceiptFooter(),
                settings.getReceiptLogoUrl(),
                settings.getBusinessMode(),
                settings.getUpdatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
