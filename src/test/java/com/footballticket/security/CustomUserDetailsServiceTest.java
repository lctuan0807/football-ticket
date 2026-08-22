package com.footballticket.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.footballticket.entity.UserEntity;
import com.footballticket.enums.UserRoleEnum;
import com.footballticket.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  private CustomUserDetailsService service;

  @Test
  void loadUserByUsername_returnsUserAuthority_whenRoleIsNull() {
    service = new CustomUserDetailsService(userRepository);
    UserEntity user = new UserEntity();
    user.setUsername("tuanle");
    user.setPassword("hashed-password");
    user.setRole(null);
    given(userRepository.findByUsername("tuanle")).willReturn(Optional.of(user));

    UserDetails result = service.loadUserByUsername("tuanle");

    assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
  }

  @Test
  void loadUserByUsername_returnsAdminAuthority_whenRoleIsAdmin() {
    service = new CustomUserDetailsService(userRepository);
    UserEntity user = new UserEntity();
    user.setUsername("admin");
    user.setPassword("hashed-password");
    user.setRole(UserRoleEnum.ADMIN.toInt());
    given(userRepository.findByUsername("admin")).willReturn(Optional.of(user));

    UserDetails result = service.loadUserByUsername("admin");

    assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
  }

  @Test
  void loadUserByUsername_throwsUsernameNotFoundException_whenUserMissing() {
    service = new CustomUserDetailsService(userRepository);
    given(userRepository.findByUsername("missing")).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserByUsername("missing"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
