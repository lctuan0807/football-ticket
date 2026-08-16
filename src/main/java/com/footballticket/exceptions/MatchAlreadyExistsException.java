package com.footballticket.exceptions;

public class MatchAlreadyExistsException extends RuntimeException {
  public MatchAlreadyExistsException(String message) {
    super(message);
  }
}
