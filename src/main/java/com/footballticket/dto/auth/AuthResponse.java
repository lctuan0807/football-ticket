package com.footballticket.dto.auth;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {
}
