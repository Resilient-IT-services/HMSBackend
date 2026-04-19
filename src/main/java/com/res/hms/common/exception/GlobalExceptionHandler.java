package com.res.hms.common.exception;

import com.res.hms.common.api.ApiError;
import com.res.hms.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(
            ApiResponse.failure(
                new ApiError(exception.getCode(), exception.getMessage(), exception.getDetails())));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @SuppressWarnings("PMD.UseConcurrentHashMap")
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException exception) {
    Map<String, Object> details = new HashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      String message = fieldError.getDefaultMessage();
      details.put(fieldError.getField(), message != null ? message : "Invalid value");
    }

    return ResponseEntity.badRequest()
        .body(ApiResponse.failure(new ApiError("VALIDATION_ERROR", "Validation failed", details)));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @SuppressWarnings("PMD.UseConcurrentHashMap")
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException exception) {
    Map<String, Object> details = new HashMap<>();
    exception
        .getConstraintViolations()
        .forEach(
            violation -> {
              String message = violation.getMessage();
              details.put(
                  violation.getPropertyPath().toString(),
                  message != null ? message : "Invalid value");
            });

    return ResponseEntity.badRequest()
        .body(ApiResponse.failure(new ApiError("VALIDATION_ERROR", "Validation failed", details)));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.failure(new ApiError("FORBIDDEN", "Access denied", Map.of())));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.failure(
                new ApiError("SYSTEM_ERROR", "Unexpected error occurred", Map.of())));
  }
}
