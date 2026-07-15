package com.prodwatch.api.service;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.user.AdminStepDownRequest;
import com.prodwatch.api.dto.user.UserProvisionRequest;
import com.prodwatch.api.dto.user.UserResetPasswordRequest;
import com.prodwatch.api.dto.user.UserResponse;
import com.prodwatch.api.dto.user.UserUpdateRequest;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.TransactionRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@Service
public class UserService {

    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final TransactionRepository transactionRepository;
    private final SupabaseAuthService supabaseAuthService;
    private final AuditService auditService;
    private final EntityManager entityManager;

    public UserService(
            ProfileRepository profileRepository,
            UserRoleRepository userRoleRepository,
            TransactionRepository transactionRepository,
            SupabaseAuthService supabaseAuthService,
            AuditService auditService,
            EntityManager entityManager) {
        this.profileRepository = profileRepository;
        this.userRoleRepository = userRoleRepository;
        this.transactionRepository = transactionRepository;
        this.supabaseAuthService = supabaseAuthService;
        this.auditService = auditService;
        this.entityManager = entityManager;
    }

    public List<UserResponse> list() {
        return profileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse get(UUID id) {
        return toResponse(loadProfile(id));
    }

    public UserResponse getMe(CurrentUser currentUser) {
        return get(currentUser.getUserId());
    }

    @Transactional
    public UserResponse provision(UserProvisionRequest dto, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        if (dto.role() == AppRole.admin) {
            throw new BusinessRuleException("Cannot provision admin via this endpoint");
        }
        if (profileRepository.existsByEmail(dto.email())) {
            throw new BusinessRuleException("Email already registered");
        }

        UUID userId = supabaseAuthService.createConfirmedUser(dto.email(), dto.password(), dto.name());
        Profile profile = ensureProfile(userId, dto.email(), dto.name(), dto.active());
        userRoleRepository.save(new UserRole(profile, dto.role()));

        auditService.log(admin.getUserId(), "USER_PROVISIONED", "user", userId, dto.email(), dto.name().trim());
        return toResponse(profile);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest dto, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        Profile profile = loadProfile(id);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));

        if (dto.active() != null) {
            profile.setActive(dto.active());
            if (dto.active()) {
                supabaseAuthService.enableUser(id);
            } else {
                assertNotLastAdmin(id, userRole.getRole());
                supabaseAuthService.disableUser(id);
            }
        }

        if (dto.role() != null) {
            if (dto.role() == AppRole.admin) {
                throw new BusinessRuleException("Use promote-admin to grant admin role");
            }
            userRole.setRole(dto.role());
            userRoleRepository.save(userRole);
            auditService.log(
                    admin.getUserId(), "ROLE_CHANGED", "user", id, dto.role().name(), profile.getName());
        }

        profileRepository.save(profile);
        return toResponse(profile);
    }

    @Transactional
    public UserResponse promoteToAdmin(UUID id, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        Profile profile = loadProfile(id);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));

        if (userRole.getRole() == AppRole.admin) {
            return toResponse(profile);
        }

        userRole.setRole(AppRole.admin);
        userRoleRepository.save(userRole);
        auditService.log(
                admin.getUserId(), "ROLE_PROMOTED_TO_ADMIN", "user", id, profile.getEmail(), profile.getName());
        return toResponse(profile);
    }

    @Transactional
    public UserResponse stepDownFromAdmin(AdminStepDownRequest dto, CurrentUser actor) {
        if (dto.role() == AppRole.admin) {
            throw new BusinessRuleException("Cannot step down to admin role");
        }

        Profile profile = loadProfile(actor.getUserId());
        if (!profile.isActive()) {
            throw new BusinessRuleException("Inactive users cannot step down from admin");
        }

        UserRole userRole = userRoleRepository
                .findByUser_Id(actor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));

        if (userRole.getRole() != AppRole.admin) {
            throw new BusinessRuleException("Only admins can step down from admin");
        }

        if (userRoleRepository.countActiveByRole(AppRole.admin) <= 1) {
            throw new BusinessRuleException("Cannot step down while you are the only active admin");
        }

        userRole.setRole(dto.role());
        userRoleRepository.save(userRole);
        auditService.log(
                actor.getUserId(),
                "ROLE_STEPPED_DOWN_FROM_ADMIN",
                "user",
                actor.getUserId(),
                dto.role().name(),
                profile.getName());
        return toResponse(profile);
    }

    @Transactional
    public void resetPassword(UUID id, UserResetPasswordRequest dto, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));
        Profile profile = loadProfile(id);
        assertCanResetPassword(admin.getUserId(), id, userRole.getRole());
        supabaseAuthService.updatePassword(id, dto.newPassword());
        auditService.log(admin.getUserId(), "PASSWORD_RESET_BY_ADMIN", "user", id, null, profile.getName());
    }

    @Transactional
    public UserResponse deactivate(UUID id, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        Profile profile = loadProfile(id);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));
        assertNotLastAdmin(id, userRole.getRole());
        profile.setActive(false);
        supabaseAuthService.disableUser(id);
        profileRepository.save(profile);
        return toResponse(profile);
    }

    @Transactional
    public UserResponse reactivate(UUID id, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        Profile profile = loadProfile(id);
        profile.setActive(true);
        supabaseAuthService.enableUser(id);
        profileRepository.save(profile);
        return toResponse(profile);
    }

    @Transactional
    public void delete(UUID id, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        if (admin.getUserId().equals(id)) {
            throw new BusinessRuleException("You cannot delete your own account");
        }

        Profile profile = loadProfile(id);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));

        if (userRole.getRole() == AppRole.admin) {
            throw new BusinessRuleException("Cannot delete an admin account");
        }

        if (transactionRepository.existsByCashier_Id(id)) {
            throw new BusinessRuleException(
                    "Cannot delete user with POS transaction history. Deactivate the account instead.");
        }

        auditService.log(admin.getUserId(), "USER_DELETED", "user", id, profile.getEmail(), profile.getName());

        // Supabase auth delete cascades to profiles + user_roles in Postgres.
        supabaseAuthService.deleteUser(id);

        entityManager.detach(profile);
        entityManager.detach(userRole);

        // H2 tests have no auth.users — remove app rows when cascade did not run.
        if (profileRepository.existsById(id)) {
            userRoleRepository.findByUser_Id(id).ifPresent(userRoleRepository::delete);
            profileRepository.deleteById(id);
        }
    }

    private Profile loadProfile(UUID id) {
        return profileRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Profile ensureProfile(UUID userId, String email, String name, boolean active) {
        return profileRepository.findById(userId).orElseGet(() -> profileRepository.save(
                Profile.create(userId, email, name, active)));
    }

    private UserResponse toResponse(Profile profile) {
        AppRole role = userRoleRepository
                .findByUser_Id(profile.getId())
                .map(UserRole::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));
        return new UserResponse(
                profile.getId(), profile.getName(), profile.getEmail(), role, profile.isActive(), null);
    }

    private void assertNotLastAdmin(UUID userId, AppRole role) {
        if (role == AppRole.admin && userRoleRepository.countActiveByRole(AppRole.admin) <= 1) {
            throw new BusinessRuleException("Cannot deactivate the last active admin");
        }
    }

    private void assertCanResetPassword(UUID actorId, UUID targetId, AppRole targetRole) {
        if (targetRole == AppRole.admin && !actorId.equals(targetId)) {
            throw new BusinessRuleException("Cannot reset password for another admin");
        }
    }
}
