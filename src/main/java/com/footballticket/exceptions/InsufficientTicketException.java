package com.footballticket.exceptions;

public class InsufficientTicketException extends RuntimeException {
  public InsufficientTicketException(String message) {
    super(message);
  }
}
