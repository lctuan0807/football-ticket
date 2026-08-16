package com.footballticket.service.impl;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.footballticket.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
  private final RedisTemplate<String, Object> redisTemplate;

  public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

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
    if (value == null) {
      return null;
    }

    if (targetClass.isInstance(value)) {
      return targetClass.cast(value);
    }

    if (value instanceof Map) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(value, targetClass);
      } catch (Exception e) {
        log.error("Error converting LinkedHashMap to object: {}", e.getMessage());
        return null;
      }
    }

    if (value instanceof String) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue((String) value, targetClass);
      } catch (Exception e) {
        log.error("Error deserializing JSON to object: {}", e.getMessage());
        return null;
      }
    }

    return null;
  }

  @Override
  public void delete(String key) {
    redisTemplate.delete(key);
  }
}
