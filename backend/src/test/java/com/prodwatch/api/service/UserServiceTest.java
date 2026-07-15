package com.prodwatch.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.prodwatch.api.dto.user.AdminStepDownRequest;
import com.prodwatch.api.dto.user.UserProvisionRequest;
import com.prodwatch.api.dto.user.UserResetPasswordRequest;
import com.prodwatch.api.dto.user.UserUpdateRequest;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.repository.EcosystemRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.support.AbstractIntegrationTest;
import com.prodwatch.api.support.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EcosystemRepository ecosystemRepository;

    @MockBean
    private SupabaseAuthService supabaseAuthService;

    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        TestFixtures.seedUser(profileRepository, userRoleRepository, ecosystemRepository, TestFixtures.ADMIN_ID, AppRole.admin);
        admin = TestFixtures.currentUser(TestFixtures.ADMIN_ID, AppRole.admin);
        when(supabaseAuthService.createConfirmedUser(anyString(), anyString(), anyString()))
                .thenReturn(UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc1"));
    }

    @Test
    void provisionRejectsAdminRole() {
        UserProvisionRequest request = new UserProvisionRequest(
                "new@test.local", "New User", "Password123!", AppRole.admin, true);

        assertThatThrownBy(() -> userService.provision(request, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot provision admin");
    }

    @Test
    void provisionCreatesConfirmedUser() {
        UserProvisionRequest request = new UserProvisionRequest(
                "worker@test.local", "Worker", "Password123!", AppRole.warehouse_worker, true);

        var response = userService.provision(request, admin);

        assertThat(response.email()).isEqualTo("worker@test.local");
        assertThat(response.role()).isEqualTo(AppRole.warehouse_worker);
        assertThat(response.active()).isTrue();
        assertThat(response.ecosystemId()).isEqualTo(TestFixtures.DEMO_ECOSYSTEM_ID);
        assertThat(response.ecosystemName()).isEqualTo("Acme Demo Test");
        assertThat(profileRepository
                        .findById(UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc1"))
                        .orElseThrow()
                        .getEcosystemId())
                .isEqualTo(TestFixtures.DEMO_ECOSYSTEM_ID);
    }

    @Test
    void cannotAssignAdminViaPatch() {
        Profile worker = TestFixtures.saveProfile(
                profileRepository,
                ecosystemRepository,
                UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                "patch@test.local",
                "Patch User",
                true);
        userRoleRepository.save(new UserRole(worker, AppRole.warehouse_worker));

        assertThatThrownBy(() -> userService.update(
                        worker.getId(), new UserUpdateRequest(null, AppRole.admin), admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("promote-admin");
    }

    @Test
    void promoteToAdminWorks() {
        Profile worker = TestFixtures.saveProfile(
                profileRepository,
                ecosystemRepository,
                UUID.fromString("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"),
                "promote@test.local",
                "Promote User",
                true);
        userRoleRepository.save(new UserRole(worker, AppRole.warehouse_worker));

        var response = userService.promoteToAdmin(worker.getId(), admin);

        assertThat(response.role()).isEqualTo(AppRole.admin);
    }

    @Test
    void cannotDeactivateLastAdmin() {
        assertThatThrownBy(() -> userService.deactivate(TestFixtures.ADMIN_ID, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last active admin");
    }

    @Test
    void adminCanResetOwnPassword() {
        userService.resetPassword(
                TestFixtures.ADMIN_ID, new UserResetPasswordRequest("NewPassword123!"), admin);
    }

    @Test
    void adminCannotResetOtherAdminPassword() {
        UUID otherAdminId = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        TestFixtures.saveProfile(
                profileRepository, ecosystemRepository, otherAdminId, "other-admin@test.local", "Other Admin", true);
        userRoleRepository.save(new UserRole(profileRepository.getReferenceById(otherAdminId), AppRole.admin));

        assertThatThrownBy(() -> userService.resetPassword(
                        otherAdminId, new UserResetPasswordRequest("NewPassword123!"), admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("another admin");
    }

    @Test
    void adminCanStepDownWhenAnotherActiveAdminExists() {
        UUID otherAdminId = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        TestFixtures.saveProfile(
                profileRepository, ecosystemRepository, otherAdminId, "other-admin@test.local", "Other Admin", true);
        userRoleRepository.save(new UserRole(profileRepository.getReferenceById(otherAdminId), AppRole.admin));

        var response = userService.stepDownFromAdmin(new AdminStepDownRequest(AppRole.inspector), admin);

        assertThat(response.role()).isEqualTo(AppRole.inspector);
    }

    @Test
    void adminCannotStepDownAsOnlyActiveAdmin() {
        assertThatThrownBy(() -> userService.stepDownFromAdmin(new AdminStepDownRequest(AppRole.inspector), admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only active admin");
    }

    @Test
    void nonAdminCannotStepDown() {
        Profile worker = TestFixtures.saveProfile(
                profileRepository,
                ecosystemRepository,
                UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff"),
                "worker-step@test.local",
                "Worker Step",
                true);
        userRoleRepository.save(new UserRole(worker, AppRole.warehouse_worker));
        CurrentUser workerUser = TestFixtures.currentUser(worker.getId(), AppRole.warehouse_worker);

        assertThatThrownBy(() -> userService.stepDownFromAdmin(new AdminStepDownRequest(AppRole.inspector), workerUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only admins can step down");
    }

    @Test
    void adminCanDeleteNonAdminUser() {
        Profile worker = TestFixtures.saveProfile(
                profileRepository,
                ecosystemRepository,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "delete@test.local",
                "Delete Me",
                true);
        userRoleRepository.save(new UserRole(worker, AppRole.cashier));

        userService.delete(worker.getId(), admin);

        assertThat(profileRepository.findById(worker.getId())).isEmpty();
        assertThat(userRoleRepository.findByUser_Id(worker.getId())).isEmpty();
    }

    @Test
    void adminCannotDeleteAnotherAdmin() {
        UUID otherAdminId = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        Profile otherAdmin = TestFixtures.saveProfile(
                profileRepository, ecosystemRepository, otherAdminId, "other-admin@test.local", "Other Admin", true);
        userRoleRepository.save(new UserRole(otherAdmin, AppRole.admin));

        assertThatThrownBy(() -> userService.delete(otherAdminId, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot delete an admin");
    }

    @Test
    void adminCannotDeleteSelf() {
        assertThatThrownBy(() -> userService.delete(TestFixtures.ADMIN_ID, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("your own account");
    }
}
