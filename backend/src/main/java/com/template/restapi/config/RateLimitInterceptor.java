/**
 * #############################################################################
 * ### Rate-limit interceptor
 * ###
 * ### @file RateLimitInterceptor.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Spring MVC interceptor that enforces @RateLimit on controller handlers.
 * Before a handler runs, it:
 *   1. Reads the @RateLimit("<endpoint_key>") annotation on the method.
 *   2. Looks up request_limit + unit_of_time_for_limit from config_file.json
 *      (via ConfigLoader) for that key.
 *   3. Asks the shared RateLimiter whether the caller's IP is within the limit.
 *   4. Throws RateLimitExceededException (-> 429) when the limit is exceeded.
 *
 * Together with RateLimiter (the shared limiter) and the GlobalExceptionHandler
 * (the 429 responder).
 */
package com.template.restapi.config;

import java.time.Duration;

import com.template.restapi.core_specs.configuration.ConfigLoader;
import com.template.restapi.error.RateLimitExceededException;
import com.template.restapi.util.CustomLogger;
import com.template.restapi.util.RateLimiter;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only method handlers can carry the annotation; let other handlers pass.
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (annotation == null) {
            return true; // not rate-limited
        }

        String endpointKey = annotation.value();
        JsonNode cfg = ConfigLoader.endpoint(endpointKey);
        int limit = cfg.path("request_limit").asInt(60);
        Duration window = RateLimiter.windowFor(cfg.path("unit_of_time_for_limit").asText("m"));

        String clientIp = resolveClientIp(request);

        if (!rateLimiter.isAllowed(clientIp, endpointKey, limit, window, System.nanoTime())) {
            CustomLogger.warning("Rate limit exceeded for IP: " + clientIp + " on '" + endpointKey + "'");
            throw new RateLimitExceededException(
                    "Request rate limit exceeded. Please try again later.");
        }
        return true;
    }

    /** Honors X-Forwarded-For (set proxy_headers in production) then falls back. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First hop is the original client.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
