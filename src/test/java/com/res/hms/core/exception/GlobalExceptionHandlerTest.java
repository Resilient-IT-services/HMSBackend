package com.res.hms.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleApiExceptionUsesStatusAndPayload() {
    UUID id = UUID.randomUUID();
    ResponseEntity<?> response =
        handler.handleApiException(
            new ApiException("NOT_FOUND", "Missing", HttpStatus.NOT_FOUND, Map.of("id", id)));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals(
        "NOT_FOUND", ((com.res.hms.core.api.ApiResponse<?>) response.getBody()).error().code());
  }

  @Test
  void handleValidationExceptionReturnsFieldMessages() throws Exception {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "username", "username is required"));
    Method method = SampleController.class.getDeclaredMethod("sample", String.class);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

    ResponseEntity<?> response = handler.handleValidationException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(
        "username is required",
        ((com.res.hms.core.api.ApiResponse<?>) response.getBody())
            .error()
            .details()
            .get("username"));
  }

  @Test
  void handleConstraintViolationReturnsViolationDetails() {
    @SuppressWarnings("unchecked")
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("create.username");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");

    ResponseEntity<?> response =
        handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(
        "must not be blank",
        ((com.res.hms.core.api.ApiResponse<?>) response.getBody())
            .error()
            .details()
            .get("create.username"));
  }

  @Test
  void handleAccessDeniedReturnsForbidden() {
    ResponseEntity<?> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals(
        "FORBIDDEN", ((com.res.hms.core.api.ApiResponse<?>) response.getBody()).error().code());
  }

  @Test
  void handleUnhandledReturnsInternalServerError() {
    ResponseEntity<?> response = handler.handleUnhandled(new IllegalStateException("boom"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    com.res.hms.core.api.ApiResponse<?> body =
        (com.res.hms.core.api.ApiResponse<?>) response.getBody();
    assertEquals("SYSTEM_ERROR", body.error().code());
    assertEquals("An unexpected error occurred", body.error().message());
  }

  @Test
  void handleUnhandledDoesNotLeakMessage() {
    String secretMessage = "Internal database connection error at 192.168.1.100";
    ResponseEntity<?> response = handler.handleUnhandled(new RuntimeException(secretMessage));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    com.res.hms.core.api.ApiResponse<?> body =
        (com.res.hms.core.api.ApiResponse<?>) response.getBody();
    assertEquals("SYSTEM_ERROR", body.error().code());
    assertEquals("An unexpected error occurred", body.error().message());
    assertEquals(0, body.error().details().size());
  }

  @Test
  void handleDataIntegrityViolationReturnsConflict() {
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException(
            "Conflict",
            new RuntimeException(
                "duplicate key value violates unique constraint \"user_account_username_key\""));
    ResponseEntity<?> response = handler.handleDataIntegrityViolation(exception);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(
        "Username already exists",
        ((com.res.hms.core.api.ApiResponse<?>) response.getBody()).error().message());
  }

  @Test
  void handleDataIntegrityViolationReturnsGenericConflict() {
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException(
            "Conflict", new RuntimeException("some other constraint"));
    ResponseEntity<?> response = handler.handleDataIntegrityViolation(exception);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(
        "A data integrity violation occurred",
        ((com.res.hms.core.api.ApiResponse<?>) response.getBody()).error().message());
  }

  static class SampleController {
    void sample(String username) {}
  }
}
