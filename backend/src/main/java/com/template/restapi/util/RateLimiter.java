/**
 * #############################################################################
 * ### Limiter element file
 * ###
 * ### @file RateLimiter.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Shared, in-memory rate limiter (the shared SlowAPI Limiter instance).
 *
 * It implements a fixed-window counter keyed by (clientIp + endpointKey):
 * each client gets up to `limit` requests per window; once exceeded, calls
 * return false until the window rolls over. No external dependency (pure JDK),
 * matching the "no Redis required for the template" stance of the Python side.
 *
 * For a single instance / small template this is plenty. For multi-instance
 * deployments swap the in-memory map for Redis (see README "Scaling").
 */
package com.template.restapi.util;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RateLimiter {

    /** One counting window for a given client+endpoint pair. */
    private static final class Window {
        volatile long windowStartNanos;
        final AtomicInteger count = new AtomicInteger(0);
    }

    // key -> current window. Created lazily on first request from that key.
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Records a request and reports whether it is allowed.
     *
     * @param clientKey   stable client identifier (typically the remote IP)
     * @param endpointKey config endpoints.* key, so different routes count separately
     * @param limit       max requests permitted within the window
     * @param window      length of the window
     * @param nowNanos    current System.nanoTime() (passed in for testability)
     * @return true if the request is within the limit, false if it should be rejected
     */
    public boolean isAllowed(String clientKey, String endpointKey, int limit,
                             Duration window, long nowNanos) {
        String key = clientKey + "|" + endpointKey;
        long windowNanos = window.toNanos();

        Window w = windows.computeIfAbsent(key, k -> {
            Window created = new Window();
            created.windowStartNanos = nowNanos;
            return created;
        });

        synchronized (w) {
            if (nowNanos - w.windowStartNanos >= windowNanos) {
                // Window expired: start a fresh one.
                w.windowStartNanos = nowNanos;
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= limit;
        }
    }

    /** Maps a unit token from config ("s", "m", "h", "d") to a Duration. */
    public static Duration windowFor(String unitOfTime) {
        if (unitOfTime == null || unitOfTime.isEmpty()) {
            return Duration.ofMinutes(1);
        }
        switch (unitOfTime.trim().toLowerCase().charAt(0)) {
            case 's': return Duration.ofSeconds(1);
            case 'h': return Duration.ofHours(1);
            case 'd': return Duration.ofDays(1);
            case 'm':
            default:  return Duration.ofMinutes(1);
        }
    }
}
