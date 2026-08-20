package com.prodwatch.api.service;

import java.util.UUID;

import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Ecosystem;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthBootstrapService {

    private final UserRoleRepository userRoleRepository;
    private final ProfileRepository profileRepository;
    private final EcosystemService ecosystemService;
    private final AuthContextService authContextService;

    public AuthBootstrapService(
            UserRoleRepository userRoleRepository,
            ProfileRepository profileRepository,
            EcosystemService ecosystemService,
            AuthContextService authContextService) {
        this.userRoleRepository = userRoleRepository;
        this.profileRepository = profileRepository;
        this.ecosystemService = ecosystemService;
        this.authContextService = authContextService;
    }

    /** Public sign-up is always available; each confirmed sign-up receives the admin role. */
    public boolean isSignupAllowed() {
        return true;
    }

    @Transactional
    public void assignAdminFromSignup(UUID userId) {
        createEcosystemIfAbsent(userId);
        assignAdminRole(userId);
        authContextService.evict(userId);
    }

    /** Public sign-up users receive admin; idempotent if role already exists. */
    @Transactional
    public void ensureAdminRoleFromSignup(UUID userId) {
        createEcosystemIfAbsent(userId);
        if (userRoleRepository.findByUser_Id(userId).isPresent()) {
            authContextService.evict(userId);
            return;
        }
        assignAdminRole(userId);
        authContextService.evict(userId);
    }

    @Transactional
    public Ecosystem createEcosystemIfAbsent(UUID userId) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Profile not found for user"));

        if (profile.getEcosystem() != null) {
            return profile.getEcosystem();
        }

        return ecosystemService.createForOwner(userId, profile.getName() + "'s workspace");
    }

    private void assignAdminRole(UUID userId) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Profile not found for user"));

        userRoleRepository
                .findByUser_Id(userId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setRole(AppRole.admin);
                            userRoleRepository.save(existing);
                        },
                        () -> userRoleRepository.save(new UserRole(profile, AppRole.admin)));
    }
}
