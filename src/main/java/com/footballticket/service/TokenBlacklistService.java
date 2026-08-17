package com.footballticket.service;

import java.time.Duration;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.footballticket.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {
  private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

  private final RedisService redisService;
  private final JwtService jwtService;

  public void blacklist(String token) {
    try {
      Date expiration = jwtService.extractExpiration(token);
      long ttlMillis = expiration.getTime() - System.currentTimeMillis();
      if (ttlMillis > 0) {
        redisService.setObject(getCacheKey(token), true, Duration.ofMillis(ttlMillis));
      }
    } catch (Exception e) {
      log.warn("TokenBlacklistService | blacklist | Failed to parse token, ignoring: {}", e.getMessage());
    }
  }

  public boolean isBlacklisted(String token) {
    return redisService.getObject(getCacheKey(token), Boolean.class) != null;
  }

  private String getCacheKey(String token) {
    return BLACKLIST_KEY_PREFIX + token;
  }
}
