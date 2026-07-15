package com.prodwatch.api.service;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.user.AdminStepDownRequest;
import com.prodwatch.api.dto.user.UserProvisionRequest;
import com.prodwatch.api.dto.user.UserResetPasswordRequest;
import com.prodwatch.api.dto.user.UserResponse;
import com.prodwatch.api.dto.user.UserUpdateRequest;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Ecosystem;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.TransactionRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;
import com.prodwatch.api.security.TenantContext;

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
    private final EcosystemService ecosystemService;
    private final EntityManager entityManager;

    public UserService(
            ProfileRepository profileRepository,
            UserRoleRepository userRoleRepository,
            TransactionRepository transactionRepository,
            SupabaseAuthService supabaseAuthService,
            AuditService auditService,
            EcosystemService ecosystemService,
            EntityManager entityManager) {
        this.profileRepository = profileRepository;
        this.userRoleRepository = userRoleRepository;
        this.transactionRepository = transactionRepository;
        this.supabaseAuthService = supabaseAuthService;
        this.auditService = auditService;
        this.ecosystemService = ecosystemService;
        this.entityManager = entityManager;
    }

    public List<UserResponse> list(CurrentUser admin) {
        UUID ecosystemId = TenantContext.requireEcosystemId(admin);
        return profileRepository.findAllByEcosystem_Id(ecosystemId).stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse get(UUID id, CurrentUser admin) {
        return toResponse(loadProfile(id, admin));
    }

    public UserResponse getMe(CurrentUser currentUser) {
        return get(currentUser.getUserId(), currentUser);
    }

    @Transactional
    public UserResponse provision(UserProvisionRequest dto, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        UUID ecosystemId = TenantContext.requireEcosystemId(admin);
        if (dto.role() == AppRole.admin) {
            throw new BusinessRuleException("Cannot provision admin via this endpoint");
        }
        if (profileRepository.existsByEmail(dto.email())) {
            throw new BusinessRuleException("Email already registered");
        }

        UUID userId = supabaseAuthService.createConfirmedUser(dto.email(), dto.password(), dto.name());
        Ecosystem ecosystem = ecosystemService.requireById(ecosystemId);
        Profile profile = ensureProfile(userId, dto.email(), dto.name(), dto.active(), ecosystem);
        userRoleRepository.save(new UserRole(profile, dto.role()));

        auditService.log(admin.getUserId(), "USER_PROVISIONED", "user", userId, dto.email(), dto.name().trim());
        return toResponse(profile);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest dto, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        UUID ecosystemId = TenantContext.requireEcosystemId(admin);
        Profile profile = loadProfile(id, admin);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));

        if (dto.active() != null) {
            profile.setActive(dto.active());
            if (dto.active()) {
                supabaseAuthService.enableUser(id);
            } else {
                assertNotLastAdmin(id, userRole.getRole(), ecosystemId);
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
        Profile profile = loadProfile(id, admin);
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

        UUID ecosystemId = TenantContext.requireEcosystemId(actor);
        Profile profile = loadProfile(actor.getUserId(), actor);
        if (!profile.isActive()) {
            throw new BusinessRuleException("Inactive users cannot step down from admin");
        }

        UserRole userRole = userRoleRepository
                .findByUser_Id(actor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));

        if (userRole.getRole() != AppRole.admin) {
            throw new BusinessRuleException("Only admins can step down from admin");
        }

        if (userRoleRepository.countActiveByRoleAndEcosystemId(AppRole.admin, ecosystemId) <= 1) {
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
        Profile profile = loadProfile(id, admin);
        assertCanResetPassword(admin.getUserId(), id, userRole.getRole());
        supabaseAuthService.updatePassword(id, dto.newPassword());
        auditService.log(admin.getUserId(), "PASSWORD_RESET_BY_ADMIN", "user", id, null, profile.getName());
    }

    @Transactional
    public UserResponse deactivate(UUID id, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        UUID ecosystemId = TenantContext.requireEcosystemId(admin);
        Profile profile = loadProfile(id, admin);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));
        assertNotLastAdmin(id, userRole.getRole(), ecosystemId);
        profile.setActive(false);
        supabaseAuthService.disableUser(id);
        profileRepository.save(profile);
        return toResponse(profile);
    }

    @Transactional
    public UserResponse reactivate(UUID id, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        Profile profile = loadProfile(id, admin);
        profile.setActive(true);
        supabaseAuthService.enableUser(id);
        profileRepository.save(profile);
        return toResponse(profile);
    }

    @Transactional
    public void delete(UUID id, CurrentUser admin) {
        RoleChecker.requireAdmin(admin);
        UUID ecosystemId = TenantContext.requireEcosystemId(admin);
        if (admin.getUserId().equals(id)) {
            throw new BusinessRuleException("You cannot delete your own account");
        }

        Profile profile = loadProfile(id, admin);
        UserRole userRole = userRoleRepository
                .findByUser_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));

        if (userRole.getRole() == AppRole.admin) {
            throw new BusinessRuleException("Cannot delete an admin account");
        }

        if (transactionRepository.existsByEcosystem_IdAndCashier_Id(ecosystemId, id)) {
            throw new BusinessRuleException(
                    "Cannot delete user with POS transaction history. Deactivate the account instead.");
        }

        auditService.log(admin.getUserId(), "USER_DELETED", "user", id, profile.getEmail(), profile.getName());

        supabaseAuthService.deleteUser(id);

        entityManager.detach(profile);
        entityManager.detach(userRole);

        if (profileRepository.existsById(id)) {
            userRoleRepository.findByUser_Id(id).ifPresent(userRoleRepository::delete);
            profileRepository.deleteById(id);
        }
    }

    private Profile loadProfile(UUID id, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        return profileRepository
                .findByIdAndEcosystem_Id(id, ecosystemId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Profile ensureProfile(UUID userId, String email, String name, boolean active, Ecosystem ecosystem) {
        return profileRepository
                .findById(userId)
                .map(existing -> {
                    if (existing.getEcosystem() == null) {
                        existing.setEcosystem(ecosystem);
                        return profileRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> profileRepository.save(Profile.create(userId, email, name, active, ecosystem)));
    }

    private UserResponse toResponse(Profile profile) {
        AppRole role = userRoleRepository
                .findByUser_Id(profile.getId())
                .map(UserRole::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("User role not found"));
        return new UserResponse(
                profile.getId(), profile.getName(), profile.getEmail(), role, profile.isActive(), null);
    }

    private void assertNotLastAdmin(UUID userId, AppRole role, UUID ecosystemId) {
        if (role == AppRole.admin
                && userRoleRepository.countActiveByRoleAndEcosystemId(AppRole.admin, ecosystemId) <= 1) {
            throw new BusinessRuleException("Cannot deactivate the last active admin");
        }
    }

    private void assertCanResetPassword(UUID actorId, UUID targetId, AppRole targetRole) {
        if (targetRole == AppRole.admin && !actorId.equals(targetId)) {
            throw new BusinessRuleException("Cannot reset password for another admin");
        }
    }
}
