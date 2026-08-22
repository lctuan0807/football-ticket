package com.footballticket.common;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.exceptions.InvalidCredentialsException;
import com.footballticket.exceptions.InvalidReservationStateException;
import com.footballticket.exceptions.ReservationCreationFailedException;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.exceptions.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({ ResourceAlreadyExistsException.class, InsufficientTicketException.class,
      InvalidReservationStateException.class, ReservationCreationFailedException.class })
  public ResponseEntity<ApiResponse<Void>> handleConflictException(Exception e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(HttpStatus.CONFLICT, e.getMessage()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(HttpStatus.FORBIDDEN, "Access denied"));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(InvalidCredentialsException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, e.getMessage()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(HttpStatus.NOT_FOUND, e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(HttpStatus.BAD_REQUEST, message));
  }
}
