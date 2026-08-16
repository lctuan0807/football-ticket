package com.footballticket.service;

import java.time.Duration;

public interface RedisService {
  void setString(String key, String value);

  String getString(String key);

  void setObject(String key, Object value);

  void setObject(String key, Object value, Duration ttl);

  <T> T getObject(String key, Class<T> targetClass);

  void delete(String key);
}