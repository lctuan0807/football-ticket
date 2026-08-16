package com.footballticket.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "matches")
public class MatchEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String competition;

  @Column(nullable = false)
  private String stage;

  @Column(nullable = false)
  private String season;

  @Column(nullable = false)
  private String homeTeam;

  @Column(nullable = false)
  private String awayTeam;

  @Column(nullable = false)
  private LocalDateTime kickoffAt;

  @Column(nullable = false)
  private String stadium;

  @Column(nullable = false)
  private Integer status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
