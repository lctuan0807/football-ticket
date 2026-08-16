package com.footballticket.enums;

public enum MatchStatusEnum {
  SCHEDULED,
  LIVE,
  FINISHED,
  POSTPONED,
  CANCELLED;

  public int toInt() {
    return this.ordinal();
  }
}
