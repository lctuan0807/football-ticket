package com.footballticket.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.footballticket.common.ApiResponse;
import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.dto.match.UpdateMatchRequest;
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

  // Create a match
  @PostMapping
  public ResponseEntity<ApiResponse<MatchDTO>> createMatch(@RequestBody @Valid CreateMatchRequest request) {
    MatchDTO match = matchService.createMatch(request);
    return ResponseEntity.ok(ApiResponse.success("Match created successfully", match));
  }

  // Get all matches with pagination
  @GetMapping
  public ResponseEntity<ApiResponse<Page<MatchDTO>>> getAllMatches(Pageable pageable,
      @RequestParam(required = false) Integer status) {
    Page<MatchDTO> matches = matchService.getAllMatches(pageable, status);
    return ResponseEntity.ok(ApiResponse.success(matches));
  }

  // Get match by id
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<MatchDTO>> getMatch(@PathVariable Long id) {
    log.info("MatchController | getMatch | Getting match with id: {}", id);
    MatchDTO match = matchService.getMatch(id);
    return ResponseEntity.ok(ApiResponse.success(match));
  }

  // Update a match
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<MatchDTO>> updateMatch(@PathVariable Long id,
      @RequestBody @Valid UpdateMatchRequest request) {
    MatchDTO match = matchService.updateMatch(id, request);
    return ResponseEntity.ok(ApiResponse.success("Match updated successfully", match));
  }

  // Delete a match
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
    matchService.deleteMatch(id);
    return ResponseEntity.noContent().build();
  }
}
