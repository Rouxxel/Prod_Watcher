/**
 * #############################################################################
 * ### Rate-limit annotation
 * ###
 * ### @file RateLimit.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Marks a controller handler as rate-limited. The value is the key of an entry
 * under "endpoints" in config_file.json; the interceptor reads that entry's
 * request_limit and unit_of_time_for_limit at runtime.
 *
 * This is the Spring-idiomatic:
 *   @SlowLimiter.limit(f"{cfg['request_limit']}/{cfg['unit_of_time_for_limit']}")
 *
 * Usage:
 *   @RateLimit("example_endpoint_1")
 *   @GetMapping("${config.endpoints.example_endpoint_1.endpoint_route}")
 *   public List<...> list() { ... }
 */
package com.template.restapi.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** The config_file.json endpoints.* key whose limit settings apply. */
    String value();
}
