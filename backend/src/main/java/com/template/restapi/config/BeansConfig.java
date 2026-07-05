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
package com.template.restapi.config;

import com.template.restapi.util.RateLimiter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    /** The single shared rate limiter used across the whole application. */
    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }
}
