package com.wodnsivar.competitionportal.common.exception;

import com.wodnsivar.competitionportal.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(
      Exception exception, HttpServletRequest request) {
    return buildErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "Usuario o contraseña incorrectos.",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleUploadSize(
      Exception exception, HttpServletRequest request) {
    return buildErrorResponse(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "La foto debe pesar como máximo 5 MB.",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return buildErrorResponse(
        HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), null);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(
      BadRequestException exception, HttpServletRequest request) {
    return buildErrorResponse(
        HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), null);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(
      ForbiddenException exception, HttpServletRequest request) {
    return buildErrorResponse(
        HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI(), null);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(
      ConflictException exception, HttpServletRequest request) {
    return buildErrorResponse(
        HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> validationErrors = new HashMap<>();

    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), validationErrors);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(
      Exception exception, HttpServletRequest request) {
    return buildErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI(), null);
  }

  private ResponseEntity<ErrorResponse> buildErrorResponse(
      HttpStatus status, String message, String path, Map<String, String> validationErrors) {
    ErrorResponse errorResponse =
        new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            validationErrors);

    return ResponseEntity.status(status).body(errorResponse);
  }
}
