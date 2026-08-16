package com.footballticket.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.footballticket.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void setString(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
  }

  @Override
  public String getString(String key) {
    Object value = redisTemplate.opsForValue().get(key);
    return value != null ? value.toString() : null;
  }

  @Override
  public void setObject(String key, Object value) {
    redisTemplate.opsForValue().set(key, value);
  }

  @Override
  public void setObject(String key, Object value, Duration ttl) {
    redisTemplate.opsForValue().set(key, value, ttl);
  }

  @Override
  public <T> T getObject(String key, Class<T> targetClass) {
    Object value = redisTemplate.opsForValue().get(key);
    return targetClass.isInstance(value) ? targetClass.cast(value) : null;
  }

  @Override
  public void delete(String key) {
    redisTemplate.delete(key);
  }
}
