package com.footballticket.service;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;

public interface MatchService {
  MatchDTO createMatch(CreateMatchRequest request);
}
