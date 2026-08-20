package com.prodwatch.api.service;

import java.util.UUID;

import com.prodwatch.api.entity.Ecosystem;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.cache.RedisCacheKeys;
import com.prodwatch.api.cache.RedisCacheService;
import com.prodwatch.api.cache.RedisCacheTtls;
import com.prodwatch.api.repository.EcosystemRepository;
import com.prodwatch.api.repository.ProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EcosystemService {

    private final EcosystemRepository ecosystemRepository;
    private final ProfileRepository profileRepository;
    private final RedisCacheService cacheService;
    private final RedisCacheTtls cacheTtls;

    public EcosystemService(
            EcosystemRepository ecosystemRepository,
            ProfileRepository profileRepository,
            RedisCacheService cacheService,
            RedisCacheTtls cacheTtls) {
        this.ecosystemRepository = ecosystemRepository;
        this.profileRepository = profileRepository;
        this.cacheService = cacheService;
        this.cacheTtls = cacheTtls;
    }

    @Transactional
    public Ecosystem createForOwner(UUID profileId, String name) {
        Profile profile = profileRepository
                .findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (profile.getEcosystem() != null) {
            return profile.getEcosystem();
        }

        String ecosystemName = (name == null || name.isBlank()) ? "My workspace" : name.trim();
        Ecosystem ecosystem = ecosystemRepository.save(Ecosystem.create(ecosystemName));
        profile.setEcosystem(ecosystem);
        profileRepository.save(profile);
        cacheService.cacheSet(RedisCacheKeys.ecosystem(ecosystem.getId()), ecosystemName, cacheTtls.ecosystem());
        return ecosystem;
    }

    @Transactional(readOnly = true)
    public Ecosystem requireById(UUID ecosystemId) {
        return ecosystemRepository
                .findById(ecosystemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ecosystem not found"));
    }

    @Transactional(readOnly = true)
    public String getCachedName(UUID ecosystemId) {
        String cacheKey = RedisCacheKeys.ecosystem(ecosystemId);
        return cacheService
                .cacheGet(cacheKey, String.class)
                .orElseGet(() -> {
                    String name = requireById(ecosystemId).getName();
                    cacheService.cacheSet(cacheKey, name, cacheTtls.ecosystem());
                    return name;
                });
    }

    public void evictName(UUID ecosystemId) {
        cacheService.cacheDelete(RedisCacheKeys.ecosystem(ecosystemId));
    }
}
