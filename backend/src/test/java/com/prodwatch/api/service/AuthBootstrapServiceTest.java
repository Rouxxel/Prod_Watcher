package com.prodwatch.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.support.AbstractIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthBootstrapServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AuthBootstrapService authBootstrapService;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void cleanUsers() {
        userRoleRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    void signupIsAlwaysAllowed() {
        Profile adminProfile = profileRepository.save(Profile.create(
                UUID.randomUUID(), "admin@test.local", "Admin", true));
        userRoleRepository.save(new UserRole(adminProfile, AppRole.admin));

        assertThat(authBootstrapService.isSignupAllowed()).isTrue();
    }

    @Test
    void confirmedSignupBecomesAdmin() {
        UUID userId = UUID.randomUUID();
        profileRepository.save(Profile.create(userId, "owner@test.local", "Owner", true));

        authBootstrapService.assignAdminFromSignup(userId);

        assertThat(userRoleRepository.findByUser_Id(userId)).get().extracting(UserRole::getRole).isEqualTo(AppRole.admin);
    }

    @Test
    void confirmedSignupBecomesAdminEvenWhenOtherAdminsExist() {
        Profile existingAdmin = profileRepository.save(Profile.create(
                UUID.randomUUID(), "admin@test.local", "Admin", true));
        userRoleRepository.save(new UserRole(existingAdmin, AppRole.admin));

        UUID userId = UUID.randomUUID();
        profileRepository.save(Profile.create(userId, "owner2@test.local", "Owner Two", true));

        authBootstrapService.assignAdminFromSignup(userId);

        assertThat(userRoleRepository.findByUser_Id(userId)).get().extracting(UserRole::getRole).isEqualTo(AppRole.admin);
    }

    @Test
    void ensureAdminRoleFromSignupIsIdempotent() {
        UUID userId = UUID.randomUUID();
        profileRepository.save(Profile.create(userId, "owner@test.local", "Owner", true));

        authBootstrapService.ensureAdminRoleFromSignup(userId);
        authBootstrapService.ensureAdminRoleFromSignup(userId);

        assertThat(userRoleRepository.findByUser_Id(userId)).get().extracting(UserRole::getRole).isEqualTo(AppRole.admin);
    }
}
