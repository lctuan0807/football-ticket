package com.footballticket.service;

import java.time.Duration;
import java.util.Set;

public interface RedisService {
  void setString(String key, String value);

  String getString(String key);

  void setObject(String key, Object value);

  void setObject(String key, Object value, Duration ttl);

  <T> T getObject(String key, Class<T> targetClass);

  void delete(String key);

  void zAdd(String key, String member, double score);

  void zRemove(String key, String member);

  Set<String> zRangeByScore(String key, double min, double max, long limit);
}
