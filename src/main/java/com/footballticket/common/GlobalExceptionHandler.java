package com.footballticket.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.footballticket.exceptions.MatchAlreadyExistsException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MatchAlreadyExistsException.class)
  public ResponseEntity<String> handleMatchAlreadyExistsException(MatchAlreadyExistsException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
  }
}
