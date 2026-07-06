/**
 * #############################################################################
 * ### Not-found exception
 * ###
 * ### @file ResourceNotFoundException.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Thrown by services when a requested record does not exist. The
 * GlobalExceptionHandler maps it to HTTP 404 (FastAPI's HTTPException(404)
 * equivalent).
 */
package com.prodwatch.api.error;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
