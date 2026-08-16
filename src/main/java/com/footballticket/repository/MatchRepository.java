package com.footballticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballticket.entity.MatchEntity;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

}
