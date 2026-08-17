package com.footballticket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballticket.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByUsername(String username);

  boolean existsByUsername(String username);
}
