package com.footballticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.footballticket.exceptions.InvalidRefreshTokenException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock
  private RedisService redisService;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService = new RefreshTokenService(redisService);
    ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
  }

  @Test
  void issue_storesUsernameUnderGeneratedTokenWithTtl() {
    String token = refreshTokenService.issue("tuanle");

    assertThat(token).isNotBlank();
    verify(redisService).setObject(eq("refresh:token:" + token), eq("tuanle"),
        eq(Duration.ofMillis(604800000L)));
  }

  @Test
  void issue_generatesDistinctTokensAcrossCalls() {
    String first = refreshTokenService.issue("tuanle");
    String second = refreshTokenService.issue("tuanle");

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void resolveUsername_returnsUsername_whenTokenExists() {
    given(redisService.getObject("refresh:token:some-token", String.class)).willReturn("tuanle");

    String username = refreshTokenService.resolveUsername("some-token");

    assertThat(username).isEqualTo("tuanle");
  }

  @Test
  void resolveUsername_throwsInvalidRefreshTokenException_whenTokenMissing() {
    given(redisService.getObject("refresh:token:missing-token", String.class)).willReturn(null);

    assertThatThrownBy(() -> refreshTokenService.resolveUsername("missing-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void revoke_deletesTokenFromRedis() {
    refreshTokenService.revoke("some-token");

    verify(redisService).delete("refresh:token:some-token");
  }

  @Test
  void issue_neverProducesSameTokenTwice_underRepeatedCalls() {
    AtomicReference<String> lastKey = new AtomicReference<>();
    for (int i = 0; i < 20; i++) {
      String token = refreshTokenService.issue("tuanle");
      assertThat(token).isNotEqualTo(lastKey.get());
      lastKey.set(token);
    }
    verify(redisService, org.mockito.Mockito.times(20)).setObject(anyString(), eq("tuanle"), any(Duration.class));
  }
}
