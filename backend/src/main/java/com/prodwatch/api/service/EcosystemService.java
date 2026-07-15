package com.prodwatch.api.service;

import java.util.UUID;

import com.prodwatch.api.entity.Ecosystem;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.EcosystemRepository;
import com.prodwatch.api.repository.ProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EcosystemService {

    private final EcosystemRepository ecosystemRepository;
    private final ProfileRepository profileRepository;

    public EcosystemService(EcosystemRepository ecosystemRepository, ProfileRepository profileRepository) {
        this.ecosystemRepository = ecosystemRepository;
        this.profileRepository = profileRepository;
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
        return ecosystem;
    }

    @Transactional(readOnly = true)
    public Ecosystem requireById(UUID ecosystemId) {
        return ecosystemRepository
                .findById(ecosystemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ecosystem not found"));
    }
}
