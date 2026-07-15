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
 *   NoResourceFoundException          -> 404 Not Found (missing static resource / route)
 *   Exception (catch-all)             -> 500 Internal Server Error
 */
package com.prodwatch.api.error;

import com.prodwatch.api.util.CustomLogger;
import com.prodwatch.api.util.Validators;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncClientDisconnect(AsyncRequestNotUsableException ex) {
        CustomLogger.debug("Client disconnected before response completed: " + ex.getMessage());
    }

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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        if (isClientDisconnect(ex)) {
            CustomLogger.debug("Client disconnected: " + ex.getMessage());
            return null;
        }
        CustomLogger.error("Unhandled exception: " + ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private static boolean isClientDisconnect(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof AsyncRequestNotUsableException) {
                return true;
            }
            if (current instanceof IOException ioe) {
                String message = ioe.getMessage();
                if (message != null) {
                    String lower = message.toLowerCase();
                    if (lower.contains("connection reset")
                            || lower.contains("broken pipe")
                            || lower.contains("connection aborted")
                            || lower.contains("aborted by the software")) {
                        return true;
                    }
                }
            }
            if ("ClientAbortException".equals(current.getClass().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String detail) {
        if (status.is5xxServerError()) {
            CustomLogger.error(status.value() + " " + status.getReasonPhrase() + ": " + detail);
        } else {
            CustomLogger.warning(status.value() + " " + status.getReasonPhrase() + ": " + detail);
        }
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), detail);
        return ResponseEntity.status(status).body(body);
    }
}
