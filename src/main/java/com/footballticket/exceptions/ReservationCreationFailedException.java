package com.footballticket.exceptions;

public class ReservationCreationFailedException extends RuntimeException {
  public ReservationCreationFailedException(String message) {
    super(message);
  }
}
