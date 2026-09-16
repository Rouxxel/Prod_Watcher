package com.prodwatch.api.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlywayStartupSupportTest {

    @Test
    void detectsPausedOrResetConnectionAsTransient() {
        assertTrue(
                FlywayStartupSupport.isTransientDatabaseError(
                        new Exception("FATAL: terminating connection due to administrator command")));
    }

    @Test
    void detectsIoErrorMessageAsTransient() {
        assertTrue(
                FlywayStartupSupport.isTransientDatabaseError(
                        new Exception("An I/O error occurred while sending to the backend.")));
    }

    @Test
    void doesNotRetryMigrationChecksumErrors() {
        assertFalse(
                FlywayStartupSupport.isTransientDatabaseError(
                        new IllegalStateException("Validate failed: checksum mismatch")));
    }
}
