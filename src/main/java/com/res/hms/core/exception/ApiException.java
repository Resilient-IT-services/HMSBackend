package com.res.hms.core.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final String code;
  private final HttpStatus status;
  private final Map<String, Object> details;

  public ApiException(String code, String message, HttpStatus status) {
    this(code, message, status, Map.of());
  }

  public ApiException(String code, String message, HttpStatus status, Throwable cause) {
    this(code, message, status, Map.of(), cause);
  }

  public ApiException(String code, String message, HttpStatus status, Map<String, Object> details) {
    super(message);
    this.code = code;
    this.status = status;
    this.details = details;
  }

  public ApiException(
      String code,
      String message,
      HttpStatus status,
      Map<String, Object> details,
      Throwable cause) {
    super(message, cause);
    this.code = code;
    this.status = status;
    this.details = details;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
