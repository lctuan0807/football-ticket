package com.footballticket.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.footballticket.exceptions.InvalidRefreshTokenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {
  private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:token:";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final RedisService redisService;

  @Value("${jwt.refresh-expiration-ms}")
  private long refreshExpirationMs;

  // use redis TTL to expire the refresh token
  public String issue(String username) {
    String token = generateToken();
    redisService.setObject(getCacheKey(token), username, Duration.ofMillis(refreshExpirationMs));
    return token;
  }

  public String resolveUsername(String token) {
    String username = redisService.getObject(getCacheKey(token), String.class);
    if (username == null) {
      throw new InvalidRefreshTokenException("Refresh token is invalid or has expired");
    }
    return username;
  }

  public void revoke(String token) {
    redisService.delete(getCacheKey(token));
  }

  // generate a random token instead of using JWT for simplicity
  private String generateToken() {
    byte[] randomBytes = new byte[32];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String getCacheKey(String token) {
    return REFRESH_TOKEN_KEY_PREFIX + token;
  }
}
