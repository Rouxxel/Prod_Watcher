package com.prodwatch.api.config;

import com.prodwatch.api.util.CustomLogger;

import org.flywaydb.core.Flyway;

/**
 * Retries Flyway repair/migrate when Postgres is temporarily unreachable (Supabase paused,
 * pooler circuit breaker, connection reset during deploy).
 */
final class FlywayStartupSupport {

    private FlywayStartupSupport() {}

    static void repairAndMigrateWithRetry(
            Flyway flyway, int maxAttempts, long initialBackoffMs, long maxBackoffMs) {
        int attempt = 0;
        long backoffMs = initialBackoffMs;
        while (true) {
            attempt++;
            try {
                flyway.repair();
                flyway.migrate();
                if (attempt > 1) {
                    CustomLogger.info("Flyway repair/migrate succeeded on attempt " + attempt);
                }
                return;
            } catch (RuntimeException ex) {
                if (!isTransientDatabaseError(ex) || attempt >= maxAttempts) {
                    if (isTransientDatabaseError(ex)) {
                        CustomLogger.error(
                                "Database still unavailable after "
                                        + maxAttempts
                                        + " Flyway startup attempts. "
                                        + "If using Supabase, confirm the project is not paused and "
                                        + "DATABASE_URL on Render matches the current pooler password.");
                    }
                    throw ex;
                }
                CustomLogger.warning(
                        "Flyway startup failed (attempt "
                                + attempt
                                + "/"
                                + maxAttempts
                                + "): "
                                + rootMessage(ex)
                                + " — retrying in "
                                + backoffMs
                                + "ms");
                sleep(backoffMs);
                backoffMs = Math.min(maxBackoffMs, backoffMs * 2);
            }
        }
    }

    static boolean isTransientDatabaseError(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if ("org.postgresql.util.PSQLException".equals(t.getClass().getName())) {
                String sqlState = readSqlState(t);
                if (isTransientSqlState(sqlState)) {
                    return true;
                }
            }
            if (isTransientPostgresMessage(t.getMessage())) {
                return true;
            }
        }
        return false;
    }

    private static String readSqlState(Throwable psql) {
        try {
            Object state = psql.getClass().getMethod("getSQLState").invoke(psql);
            return state instanceof String s ? s : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static boolean isTransientSqlState(String sqlState) {
        if (sqlState == null) {
            return false;
        }
        return switch (sqlState) {
            case "08000", "08001", "08003", "08006", "57P01", "57P03" -> true;
            default -> false;
        };
    }

    private static boolean isTransientPostgresMessage(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("ecircuitbreaker")
                || lower.contains("terminating connection due to administrator command")
                || lower.contains("connection refused")
                || lower.contains("i/o error occurred while sending to the backend")
                || lower.contains("failed to retrieve database credentials")
                || lower.contains("too many authentication failures")
                || lower.contains("the connection attempt failed");
    }

    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : ex.getClass().getSimpleName();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry Flyway", ie);
        }
    }
}
