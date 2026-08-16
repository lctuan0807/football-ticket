package com.footballticket.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.service.MatchService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private MatchService matchService;

  @Test
  void createMatch_returnsCreatedMatch_whenRequestIsValid() throws Exception {
    CreateMatchRequest request = new CreateMatchRequest(
        "Premier League",
        "1",
        "2025/2026",
        "Arsenal",
        "Chelsea",
        LocalDateTime.of(2026, 8, 20, 18, 0),
        "Emirates Stadium");

    MatchDTO response = new MatchDTO();
    response.setId(1L);
    response.setCompetition(request.competition());
    response.setStage(request.stage());
    response.setSeason(request.season());
    response.setHomeTeam(request.homeTeam());
    response.setAwayTeam(request.awayTeam());
    response.setKickoffAt(request.kickoffAt());

    given(matchService.createMatch(any(CreateMatchRequest.class))).willReturn(response);

    mockMvc.perform(post("/api/v1/matches")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(1)))
        .andExpect(jsonPath("$.homeTeam", is("Arsenal")))
        .andExpect(jsonPath("$.awayTeam", is("Chelsea")));
  }

  @Test
  void createMatch_returnsBadRequest_whenRequiredFieldIsMissing() throws Exception {
    String requestJson = """
        {
          "competition": "Premier League",
          "stage": "1",
          "season": "2025/2026",
          "homeTeam": "Arsenal",
          "kickoffAt": "2026-08-20T18:00:00",
          "stadium": "Emirates Stadium"
        }
        """;

    mockMvc.perform(post("/api/v1/matches")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }
}
