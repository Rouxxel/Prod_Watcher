package com.prodwatch.api.support;

import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.security.CurrentUser;

public final class TestFixtures {

    public static final String JWT_SECRET = "test-jwt-secret-for-unit-tests-only-min-32-chars";

    public static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1");
    public static final UUID MANAGER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2");
    public static final UUID WORKER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3");
    public static final UUID CASHIER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa4");

    /** Fixed singleton id for workspace_settings (see V18__workspace_settings.sql). */
    public static final UUID WORKSPACE_SETTINGS_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000001");

    private TestFixtures() {}

    public static String bearerToken(UUID userId) {
        return JWT.create().withSubject(userId.toString()).sign(Algorithm.HMAC256(JWT_SECRET));
    }

    public static String bearerHeader(UUID userId) {
        return "Bearer " + bearerToken(userId);
    }

    public static Profile seedUser(ProfileRepository profiles, UserRoleRepository roles, UUID id, AppRole role) {
        Profile profile = profiles.save(Profile.create(id, role.name() + "@test.local", "Test " + role, true));
        roles.save(new UserRole(profile, role));
        return profile;
    }

    public static CurrentUser currentUser(UUID id, AppRole role) {
        return new CurrentUser(id, role.name() + "@test.local", role, true);
    }
}
