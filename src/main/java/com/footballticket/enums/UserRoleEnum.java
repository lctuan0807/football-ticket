package com.footballticket.enums;

public enum UserRoleEnum {
  USER,
  ADMIN;

  public int toInt() {
    return this.ordinal();
  }
}
