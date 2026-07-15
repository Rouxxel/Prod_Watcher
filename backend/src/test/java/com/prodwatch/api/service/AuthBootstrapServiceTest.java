package com.prodwatch.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.error.BusinessRuleException;
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
    void signupAllowedWhenNoAdminExists() {
        assertThat(authBootstrapService.isSignupAllowed()).isTrue();
    }

    @Test
    void signupBlockedAfterAdminExists() {
        Profile adminProfile = profileRepository.save(Profile.create(
                UUID.randomUUID(), "admin@test.local", "Admin", true));
        userRoleRepository.save(new UserRole(adminProfile, AppRole.admin));

        assertThat(authBootstrapService.isSignupAllowed()).isFalse();
    }

    @Test
    void firstConfirmedSignupBecomesAdmin() {
        UUID userId = UUID.randomUUID();
        profileRepository.save(Profile.create(userId, "owner@test.local", "Owner", true));

        authBootstrapService.assignAdminIfFirstUser(userId);

        assertThat(userRoleRepository.findByUser_Id(userId)).get().extracting(UserRole::getRole).isEqualTo(AppRole.admin);
    }

    @Test
    void cannotAssignAdminWhenAdminAlreadyExists() {
        Profile adminProfile = profileRepository.save(Profile.create(
                UUID.randomUUID(), "admin@test.local", "Admin", true));
        userRoleRepository.save(new UserRole(adminProfile, AppRole.admin));

        UUID userId = UUID.randomUUID();
        profileRepository.save(Profile.create(userId, "late@test.local", "Late", true));

        assertThatThrownBy(() -> authBootstrapService.assignAdminIfFirstUser(userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("admin already exists");
    }
}
