package com.footballticket.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.service.MatchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {
  private final MatchService matchService;

  @PostMapping
  public MatchDTO createMatch(@RequestBody @Valid CreateMatchRequest request) {
    try {
      MatchDTO match = matchService.createMatch(request);
      return match;
    } catch (Exception e) {
      // TODO: handle exception
      throw e;
    }
  }
}
