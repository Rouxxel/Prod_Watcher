package com.prodwatch.api.support;

import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Ecosystem;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.UserRole;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.repository.EcosystemRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.repository.WarehouseRepository;
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

    /** Demo ecosystem id from V21 backfill (Acme Demo). */
    public static final UUID DEMO_ECOSYSTEM_ID =
            UUID.fromString("33333333-3333-4333-8333-333333333301");

    /** Second tenant for cross-ecosystem isolation tests. */
    public static final UUID OTHER_ECOSYSTEM_ID =
            UUID.fromString("44444444-4444-4444-8444-444444444401");

    public static final UUID OTHER_ADMIN_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc1");

    private TestFixtures() {}

    public static String bearerToken(UUID userId) {
        return JWT.create().withSubject(userId.toString()).sign(Algorithm.HMAC256(JWT_SECRET));
    }

    public static String bearerHeader(UUID userId) {
        return "Bearer " + bearerToken(userId);
    }

    public static Ecosystem demoEcosystem(EcosystemRepository ecosystems) {
        return ecosystems
                .findById(DEMO_ECOSYSTEM_ID)
                .orElseGet(() -> ecosystems.save(Ecosystem.withId(DEMO_ECOSYSTEM_ID, "Acme Demo Test")));
    }

    public static Ecosystem otherEcosystem(EcosystemRepository ecosystems) {
        return ecosystems
                .findById(OTHER_ECOSYSTEM_ID)
                .orElseGet(() -> ecosystems.save(Ecosystem.withId(OTHER_ECOSYSTEM_ID, "Other Tenant")));
    }

    public static Profile seedUser(
            ProfileRepository profiles,
            UserRoleRepository roles,
            EcosystemRepository ecosystems,
            UUID id,
            AppRole role) {
        Ecosystem ecosystem = demoEcosystem(ecosystems);
        Profile profile = profiles.findById(id).orElseGet(() -> profiles.save(
                Profile.create(id, role.name() + "@test.local", "Test " + role, true, ecosystem)));
        if (profile.getEcosystem() == null) {
            profile.setEcosystem(ecosystem);
            profile = profiles.save(profile);
        }
        Profile savedProfile = profile;
        roles.findByUser_Id(id).orElseGet(() -> {
            roles.save(new UserRole(savedProfile, role));
            return null;
        });
        return savedProfile;
    }

    public static Profile seedUserInEcosystem(
            ProfileRepository profiles,
            UserRoleRepository roles,
            Ecosystem ecosystem,
            UUID id,
            AppRole role) {
        Profile profile = profiles.findById(id).orElseGet(() -> profiles.save(
                Profile.create(id, role.name() + "@other.local", "Other " + role, true, ecosystem)));
        if (profile.getEcosystem() == null) {
            profile.setEcosystem(ecosystem);
            profile = profiles.save(profile);
        }
        Profile savedProfile = profile;
        roles.findByUser_Id(id).orElseGet(() -> {
            roles.save(new UserRole(savedProfile, role));
            return null;
        });
        return savedProfile;
    }

    public static CurrentUser currentUser(UUID id, AppRole role) {
        return currentUser(id, DEMO_ECOSYSTEM_ID, role);
    }

    public static CurrentUser currentUser(UUID id, UUID ecosystemId, AppRole role) {
        return new CurrentUser(id, ecosystemId, role.name() + "@test.local", role, true);
    }

    public static Warehouse saveWarehouse(
            WarehouseRepository warehouses, EcosystemRepository ecosystems, String name, String location) {
        return saveWarehouse(warehouses, demoEcosystem(ecosystems), name, location);
    }

    public static Warehouse saveWarehouse(WarehouseRepository warehouses, Ecosystem ecosystem, String name, String location) {
        return warehouses.save(Warehouse.create(name, location, ecosystem));
    }

    public static Profile saveProfile(
            ProfileRepository profiles, EcosystemRepository ecosystems, UUID id, String email, String name, boolean active) {
        return profiles.save(Profile.create(id, email, name, active, demoEcosystem(ecosystems)));
    }
}
