package com.footballticket.exceptions;

public class InvalidReservationStateException extends RuntimeException {
  public InvalidReservationStateException(String message) {
    super(message);
  }
}
