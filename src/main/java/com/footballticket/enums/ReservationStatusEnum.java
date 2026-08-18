package com.footballticket.enums;

public enum ReservationStatusEnum {
  PENDING,
  CONFIRMED,
  CANCELLED,
  EXPIRED;

  public int toInt() {
    return this.ordinal();
  }
}
