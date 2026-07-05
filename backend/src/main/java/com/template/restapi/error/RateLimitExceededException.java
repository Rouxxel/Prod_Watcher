/**
 * #############################################################################
 * ### Rate-limit exception
 * ###
 * ### @file RateLimitExceededException.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Thrown by RateLimitInterceptor when a client exceeds its configured request
 * budget. The GlobalExceptionHandler maps it to HTTP 429.
 */
package com.template.restapi.error;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
