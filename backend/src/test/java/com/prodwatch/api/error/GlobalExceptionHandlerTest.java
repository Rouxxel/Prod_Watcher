package com.prodwatch.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodwatch.api.support.AbstractIntegrationTest;
import com.prodwatch.api.util.Validators;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class GlobalExceptionHandlerTest extends AbstractIntegrationTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    private void dummy(String sku) {}

    @Test
    void insufficientStockReturns409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInsufficientStock(new InsufficientStockException("need 5, have 2"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().detail()).contains("need 5");
    }

    @Test
    void forbiddenReturns403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleForbidden(new ForbiddenException("Insufficient permissions"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().status()).isEqualTo(403);
    }

    @Test
    void businessRuleReturns422() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessRule(new BusinessRuleException("Cannot delete warehouse"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().status()).isEqualTo(422);
    }

    @Test
    void notFoundReturns404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new ResourceNotFoundException("Product not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void rateLimitReturns429() {
        ResponseEntity<ErrorResponse> response =
                handler.handleRateLimit(new RateLimitExceededException("Too many requests"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().status()).isEqualTo(429);
    }

    @Test
    void beanValidationReturns400WithFieldDetails() throws NoSuchMethodException {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "target");
        binding.addError(new FieldError("target", "sku", "must not be blank"));
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("dummy", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, binding);

        ResponseEntity<ErrorResponse> response = handler.handleBeanValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().detail()).contains("sku");
    }

    @Test
    void validatorExceptionReturns400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleValidation(new Validators.ValidationException(400, "Bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unexpectedExceptionReturns500() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().detail()).isEqualTo("An unexpected error occurred.");
    }

    @Test
    void clientDisconnectDoesNotReturn500() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new AsyncRequestNotUsableException(
                        "ServletOutputStream failed to flush: java.io.IOException: "
                                + "An established connection was aborted by the software in your host machine"));

        assertThat(response).isNull();
    }
}
