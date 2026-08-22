package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @Mock
  private ZSetOperations<String, String> zSetOperations;

  private RedisServiceImpl redisService;

  @BeforeEach
  void setUp() {
    redisService = new RedisServiceImpl(redisTemplate, stringRedisTemplate);
  }

  @Test
  void setString_setsStringValue() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    redisService.setString("key", "value");

    verify(valueOperations).set("key", "value");
  }

  @Test
  void getString_returnsValue_whenKeyExists() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("key")).willReturn("value");

    String result = redisService.getString("key");

    assertThat(result).isEqualTo("value");
  }

  @Test
  void getString_returnsNull_whenKeyDoesNotExist() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("key")).willReturn(null);

    String result = redisService.getString("key");

    assertThat(result).isNull();
  }

  @Test
  void setObject_setsValue_withoutTtl() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    Object value = new Object();
    redisService.setObject("key", value);

    verify(valueOperations).set("key", value);
  }

  @Test
  void setObject_setsValue_withTtl() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    Object value = new Object();
    Duration ttl = Duration.ofMinutes(5);
    redisService.setObject("key", value, ttl);

    verify(valueOperations).set("key", value, ttl);
  }

  @Test
  void getObject_returnsTypedValue_whenKeyExistsAndTypeMatches() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("key")).willReturn("value");

    String result = redisService.getObject("key", String.class);

    assertThat(result).isEqualTo("value");
  }

  @Test
  void getObject_returnsNull_whenKeyDoesNotExist() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("key")).willReturn(null);

    String result = redisService.getObject("key", String.class);

    assertThat(result).isNull();
  }

  @Test
  void getObject_returnsNull_whenTypeDoesNotMatch() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("key")).willReturn(123);

    String result = redisService.getObject("key", String.class);

    assertThat(result).isNull();
  }

  @Test
  void delete_deletesKey() {
    redisService.delete("key");

    verify(redisTemplate).delete("key");
  }

  @Test
  void zAdd_addsMemberWithScore() {
    given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);

    redisService.zAdd("zkey", "1", 100.0);

    verify(zSetOperations).add("zkey", "1", 100.0);
  }

  @Test
  void zRemove_removesMember() {
    given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);

    redisService.zRemove("zkey", "1");

    verify(zSetOperations).remove("zkey", "1");
  }

  @Test
  void zRangeByScore_returnsMembersInScoreRange() {
    given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
    given(zSetOperations.rangeByScore("zkey", 0, 100.0, 0, 50)).willReturn(Set.of("1", "2"));

    Set<String> result = redisService.zRangeByScore("zkey", 0, 100.0, 50);

    assertThat(result).containsExactlyInAnyOrder("1", "2");
  }
}
