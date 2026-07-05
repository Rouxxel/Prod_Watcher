/**
 * #############################################################################
 * ### Error response body
 * ###
 * ### @file ErrorResponse.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Uniform JSON error shape returned by the GlobalExceptionHandler. Using a
 * single record keeps every error response consistent for API consumers.
 *
 * Example serialized body:
 *   { "status": 429, "error": "Too Many Requests",
 *     "detail": "Request rate limit exceeded. Please try again later." }
 */
package com.template.restapi.error;

public record ErrorResponse(int status, String error, String detail) {
}
