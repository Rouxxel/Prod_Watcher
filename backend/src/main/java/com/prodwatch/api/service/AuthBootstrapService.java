package com.prodwatch.api.service;

import java.util.UUID;

import com.prodwatch.api.entity.AppRole;
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

    public AuthBootstrapService(UserRoleRepository userRoleRepository, ProfileRepository profileRepository) {
        this.userRoleRepository = userRoleRepository;
        this.profileRepository = profileRepository;
    }

    /** Owner sign-up is allowed only while no admin exists in the workspace. */
    public boolean isSignupAllowed() {
        return !userRoleRepository.existsByRole(AppRole.admin);
    }

    @Transactional
    public void assignAdminIfFirstUser(UUID userId) {
        if (!isSignupAllowed()) {
            throw new BusinessRuleException("An admin already exists");
        }
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
