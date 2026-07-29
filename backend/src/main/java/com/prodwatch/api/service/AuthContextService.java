package com.prodwatch.api.service;

import java.util.Optional;
import java.util.UUID;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.prodwatch.api.cache.RedisCacheKeys;
import com.prodwatch.api.cache.RedisCacheService;
import com.prodwatch.api.cache.RedisCacheTtls;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.util.CustomLogger;

import org.springframework.stereotype.Service;

/**
 * Resolves {@link CurrentUser} for JWT auth with optional Redis cache-aside on profile + role.
 */
@Service
public class AuthContextService {

    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RedisCacheService cacheService;
    private final RedisCacheTtls cacheTtls;

    public AuthContextService(
            ProfileRepository profileRepository,
            UserRoleRepository userRoleRepository,
            RedisCacheService cacheService,
            RedisCacheTtls cacheTtls) {
        this.profileRepository = profileRepository;
        this.userRoleRepository = userRoleRepository;
        this.cacheService = cacheService;
        this.cacheTtls = cacheTtls;
    }

    /**
     * Loads the authenticated principal for a JWT subject. Returns empty when the user is
     * inactive or has no ecosystem (same rejection rules as {@code JwtAuthFilter}).
     */
    public Optional<CurrentUser> resolve(UUID userId) {
        String cacheKey = RedisCacheKeys.profile(userId);
        Optional<CachedAuthPrincipal> cached = cacheService.cacheGet(cacheKey, CachedAuthPrincipal.class);
        if (cached.isPresent()) {
            CachedAuthPrincipal principal = cached.get();
            if (!principal.active()) {
                CustomLogger.debug("Rejected inactive user (cached): " + userId);
                return Optional.empty();
            }
            if (principal.ecosystemId() == null) {
                CustomLogger.debug("Rejected user without ecosystem (cached): " + userId);
                return Optional.empty();
            }
            CustomLogger.debug("Cache hit for auth profile " + userId);
            return Optional.of(toCurrentUser(principal));
        }

        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new JWTVerificationException("Unknown user"));

        if (!profile.isActive()) {
            CustomLogger.debug("Rejected inactive user: " + userId);
            return Optional.empty();
        }

        UUID ecosystemId = profile.getEcosystemId();
        if (ecosystemId == null) {
            CustomLogger.debug("Rejected user without ecosystem: " + userId);
            return Optional.empty();
        }

        AppRole role = userRoleRepository
                .findByUser_Id(userId)
                .map(UserRole::getRole)
                .orElseThrow(() -> new JWTVerificationException("User has no role"));

        CachedAuthPrincipal toCache =
                new CachedAuthPrincipal(userId, ecosystemId, profile.getEmail(), role, profile.isActive());
        cacheService.cacheSet(cacheKey, toCache, cacheTtls.profile());
        CustomLogger.debug("Cache miss for auth profile " + userId);
        return Optional.of(toCurrentUser(toCache));
    }

    public void evict(UUID userId) {
        cacheService.cacheDelete(RedisCacheKeys.profile(userId));
    }

    private static CurrentUser toCurrentUser(CachedAuthPrincipal principal) {
        return new CurrentUser(
                principal.userId(),
                principal.ecosystemId(),
                principal.email(),
                principal.role(),
                principal.active());
    }

    /** JSON-serializable auth snapshot stored in Redis. */
    public record CachedAuthPrincipal(
            UUID userId, UUID ecosystemId, String email, AppRole role, boolean active) {}
}
