package com.footballticket.common;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  private int code;
  private String message;
  private T data;

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(HttpStatus.OK.value(), "Success", data);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(HttpStatus.OK.value(), message, data);
  }

  public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
    return new ApiResponse<>(status.value(), message, data);
  }

  public static <T> ApiResponse<T> error(HttpStatus status, String message) {
    return new ApiResponse<>(status.value(), message, null);
  }
}
