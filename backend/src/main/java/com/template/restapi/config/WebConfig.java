/**
 * #############################################################################
 * ### Web MVC configuration
 * ###
 * ### @file WebConfig.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Registers cross-cutting web concerns. Right now it wires the
 * RateLimitInterceptor into the MVC pipeline so @RateLimit is enforced on every
 * matching request. Add CORS, additional interceptors, or argument resolvers
 * here as the project grows.
 */
package com.template.restapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply to all paths; the interceptor itself no-ops on handlers without @RateLimit.
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
    }
}
