/**
 * #############################################################################
 * ### Shared beans
 * ###
 * ### @file BeansConfig.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Declares simple shared singletons that aren't component-scanned classes of
 * their own. Currently exposes the one shared RateLimiter instance so it can 
 * be injected wherever needed.
 */
package com.prodwatch.api.config;

import com.prodwatch.api.util.RateLimiter;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    /** The single shared rate limiter used across the whole application. */
    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }

    /**
     * Repair the Flyway schema history before migrating. This realigns stored checksums with the
     * migration files, which self-heals the checksum drift that Windows line-ending re-saves cause
     * for already-applied migrations. It does not re-run applied migrations.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
