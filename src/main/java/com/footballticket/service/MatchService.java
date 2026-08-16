package com.footballticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.dto.match.UpdateMatchRequest;

public interface MatchService {
  MatchDTO createMatch(CreateMatchRequest request);

  MatchDTO getMatch(Long id);

  Page<MatchDTO> getAllMatches(Pageable pageable, Integer status);

  MatchDTO updateMatch(Long id, UpdateMatchRequest request);

  void deleteMatch(Long id);
}
