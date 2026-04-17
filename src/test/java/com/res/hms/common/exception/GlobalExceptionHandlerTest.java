package com.res.hms.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiExceptionUsesStatusAndPayload() {
        ResponseEntity<?> response = handler.handleApiException(
                new ApiException("NOT_FOUND", "Missing", HttpStatus.NOT_FOUND, Map.of("id", 10L))
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", ((com.res.hms.common.api.ApiResponse<?>) response.getBody()).error().code());
    }

    @Test
    void handleValidationExceptionReturnsFieldMessages() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "username is required"));
        Method method = SampleController.class.getDeclaredMethod("sample", String.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(method, 0),
                bindingResult
        );

        ResponseEntity<?> response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "username is required",
                ((com.res.hms.common.api.ApiResponse<?>) response.getBody()).error().details().get("username")
        );
    }

    @Test
    void handleConstraintViolationReturnsViolationDetails() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("create.username");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        ResponseEntity<?> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(violation))
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "must not be blank",
                ((com.res.hms.common.api.ApiResponse<?>) response.getBody()).error().details().get("create.username")
        );
    }

    @Test
    void handleAccessDeniedReturnsForbidden() {
        ResponseEntity<?> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("FORBIDDEN", ((com.res.hms.common.api.ApiResponse<?>) response.getBody()).error().code());
    }

    @Test
    void handleUnhandledReturnsInternalServerError() {
        ResponseEntity<?> response = handler.handleUnhandled(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("SYSTEM_ERROR", ((com.res.hms.common.api.ApiResponse<?>) response.getBody()).error().code());
    }

    static class SampleController {
        void sample(String username) {
        }
    }
}
