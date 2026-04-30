package com.res.hms.common.exception;

import com.res.hms.common.api.ApiError;
import com.res.hms.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
    log.error("API Error: code={}, message={}", exception.getCode(), exception.getMessage());
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

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
      DataIntegrityViolationException exception) {
    log.error("Data integrity violation", exception);
    String message = "A data integrity violation occurred";
    String rootMsg = exception.getRootCause() != null ? exception.getRootCause().getMessage() : "";

    if (rootMsg.contains("duplicate key value violates unique constraint")) {
      message = "A record with the same unique value already exists";
      if (rootMsg.contains("doctor_license_number_key")) {
        message = "Doctor license number already exists";
      } else if (rootMsg.contains("user_account_username_key")) {
        message = "Username already exists";
      } else if (rootMsg.contains("user_account_email_key")) {
        message = "Email already exists";
      } else if (rootMsg.contains("hospital_code_key")) {
        message = "Hospital code already exists";
      } else if (rootMsg.contains("nurse_nursing_license_no_key")) {
        message = "NurseProfile license number already exists";
      } else if (rootMsg.contains("pharmacist_profile_pci_reg_no_key")) {
        message = "Pharmacist PCI registration number already exists";
      }
    }

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.failure(new ApiError("CONFLICT", message, Map.of())));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception exception) {
    log.error("Unhandled exception occurred: {}", exception.getMessage(), exception);

    Map<String, Object> details = new HashMap<>();
    if (exception.getMessage() != null) {
      details.put("message", exception.getMessage());
    }
    details.put("type", exception.getClass().getSimpleName());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.failure(
                new ApiError("SYSTEM_ERROR", "Unexpected error occurred", details)));
  }
}
