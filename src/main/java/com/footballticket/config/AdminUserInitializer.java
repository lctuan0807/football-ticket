package com.footballticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.footballticket.entity.UserEntity;
import com.footballticket.enums.UserRoleEnum;
import com.footballticket.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Registration always creates a USER; this is the only way to get an ADMIN
// account, since there's no admin-management endpoint yet.
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${admin.username}")
  private String adminUsername;

  @Value("${admin.password}")
  private String adminPassword;

  @Override
  public void run(String... args) {
    if (userRepository.existsByUsername(adminUsername)) {
      return;
    }

    UserEntity admin = new UserEntity();
    admin.setUsername(adminUsername);
    admin.setPassword(passwordEncoder.encode(adminPassword));
    admin.setRole(UserRoleEnum.ADMIN.toInt());
    userRepository.save(admin);

    log.info("Bootstrapped admin user: {}", adminUsername);
  }
}
