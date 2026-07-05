/**
 * #############################################################################
 * ### Global exception handler
 * ###
 * ### @file GlobalExceptionHandler.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Centralizes how exceptions become HTTP responses, so controllers stay clean.
 * the 429 handler plus FastAPI's built-in validation/HTTPException handling.
 *
 * Mappings:
 *   RateLimitExceededException        -> 429 Too Many Requests
 *   Validators.ValidationException    -> 400 Bad Request (carries its own code)
 *   ResourceNotFoundException         -> 404 Not Found
 *   MethodArgumentNotValidException   -> 400 Bad Request (DTO bean-validation)
 *   Exception (catch-all)             -> 500 Internal Server Error
 */
package com.template.restapi.error;

import com.template.restapi.util.CustomLogger;
import com.template.restapi.util.Validators;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(Validators.ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(Validators.ValidationException ex) {
        // The validator carries an HTTP-style status code (typically 400).
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return build(status, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        CustomLogger.error("Unhandled exception: " + ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String detail) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), detail);
        return ResponseEntity.status(status).body(body);
    }
}
