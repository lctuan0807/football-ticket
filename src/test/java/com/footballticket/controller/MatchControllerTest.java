package com.footballticket.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.dto.match.UpdateMatchRequest;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.service.MatchService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

  // @PreAuthorize needs method security enabled in this slice; the full
  // SecurityConfig (filter chain, JWT filter) isn't imported here on purpose
  // to keep this a pure controller/authorization test.
  @TestConfiguration
  @EnableMethodSecurity
  static class MethodSecurityTestConfig {
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private MatchService matchService;

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
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
        .andExpect(jsonPath("$.code", is(200)))
        .andExpect(jsonPath("$.data.id", is(1)))
        .andExpect(jsonPath("$.data.homeTeam", is("Arsenal")))
        .andExpect(jsonPath("$.data.awayTeam", is("Chelsea")));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void createMatch_returnsBadRequest_whenRequiredFieldIsMissing() throws Exception {
    String requestJson = """
        {
          "competition": "Premier League",
          "stage": "1",
          "season": "2025/2026",
          "homeTeam": "Arsenal",
          "kickoffAt": "2026-08-20 18:00:00",
          "stadium": "Emirates Stadium"
        }
        """;

    mockMvc.perform(post("/api/v1/matches")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void createMatch_returnsBadRequest_whenDateIsInvalid() throws Exception {
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

  @Test
  void getMatch_returnsMatch_whenMatchExists() throws Exception {
    MatchDTO response = new MatchDTO();
    response.setId(1L);
    response.setHomeTeam("Arsenal");
    response.setAwayTeam("Chelsea");

    given(matchService.getMatch(1L)).willReturn(response);

    mockMvc.perform(get("/api/v1/matches/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id", is(1)))
        .andExpect(jsonPath("$.data.homeTeam", is("Arsenal")))
        .andExpect(jsonPath("$.data.awayTeam", is("Chelsea")));
  }

  @Test
  void getMatch_returnsNotFound_whenMatchDoesNotExist() throws Exception {
    given(matchService.getMatch(1L)).willThrow(new ResourceNotFoundException("Match not found!"));

    mockMvc.perform(get("/api/v1/matches/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is(404)))
        .andExpect(jsonPath("$.message", is("Match not found!")));
  }

  @Test
  void getAllMatches_returnsPagedMatches_whenStatusIsNotProvided() throws Exception {
    MatchDTO match = new MatchDTO();
    match.setId(1L);
    match.setHomeTeam("Arsenal");

    Pageable pageable = PageRequest.of(0, 10);
    Page<MatchDTO> page = new PageImpl<>(List.of(match), pageable, 1);
    given(matchService.getAllMatches(any(Pageable.class), isNull())).willReturn(page);

    mockMvc.perform(get("/api/v1/matches"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id", is(1)))
        .andExpect(jsonPath("$.data.totalElements", is(1)));
  }

  @Test
  void getAllMatches_returnsFilteredMatches_whenStatusIsProvided() throws Exception {
    MatchDTO match = new MatchDTO();
    match.setId(1L);

    Pageable pageable = PageRequest.of(0, 10);
    Page<MatchDTO> page = new PageImpl<>(List.of(match), pageable, 1);
    given(matchService.getAllMatches(any(Pageable.class), eq(2))).willReturn(page);

    mockMvc.perform(get("/api/v1/matches").param("status", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id", is(1)));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void updateMatch_returnsUpdatedMatch_whenRequestIsValid() throws Exception {
    UpdateMatchRequest request = new UpdateMatchRequest(
        "La Liga",
        "2",
        "2025/2026",
        "Real Madrid",
        "Barcelona",
        LocalDateTime.of(2026, 9, 1, 20, 0),
        "Santiago Bernabeu");

    MatchDTO response = new MatchDTO();
    response.setId(1L);
    response.setHomeTeam(request.homeTeam());
    response.setAwayTeam(request.awayTeam());

    given(matchService.updateMatch(eq(1L), any(UpdateMatchRequest.class))).willReturn(response);

    mockMvc.perform(put("/api/v1/matches/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.homeTeam", is("Real Madrid")))
        .andExpect(jsonPath("$.data.awayTeam", is("Barcelona")));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void updateMatch_returnsNotFound_whenMatchDoesNotExist() throws Exception {
    UpdateMatchRequest request = new UpdateMatchRequest(
        "La Liga",
        "2",
        "2025/2026",
        "Real Madrid",
        "Barcelona",
        LocalDateTime.of(2026, 9, 1, 20, 0),
        "Santiago Bernabeu");

    given(matchService.updateMatch(eq(1L), any(UpdateMatchRequest.class)))
        .willThrow(new ResourceNotFoundException("Match not found!"));

    mockMvc.perform(put("/api/v1/matches/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void deleteMatch_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/matches/1"))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(authorities = "ROLE_USER")
  void createMatch_returnsForbidden_whenCallerIsNotAdmin() throws Exception {
    CreateMatchRequest request = new CreateMatchRequest(
        "Premier League",
        "1",
        "2025/2026",
        "Arsenal",
        "Chelsea",
        LocalDateTime.of(2026, 8, 20, 18, 0),
        "Emirates Stadium");

    mockMvc.perform(post("/api/v1/matches")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
