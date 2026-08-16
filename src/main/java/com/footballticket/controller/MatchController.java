package com.footballticket.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.service.MatchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {
  private final MatchService matchService;

  @PostMapping
  public ResponseEntity<MatchDTO> createMatch(@RequestBody @Valid CreateMatchRequest request) {
    MatchDTO match = matchService.createMatch(request);
    return ResponseEntity.ok(match);
  }
}
