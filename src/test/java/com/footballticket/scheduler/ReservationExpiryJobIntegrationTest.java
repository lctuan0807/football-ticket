package com.footballticket.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.entity.MatchEntity;
import com.footballticket.entity.ReservationEntity;
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.enums.ReservationStatusEnum;
import com.footballticket.repository.MatchRepository;
import com.footballticket.repository.ReservationRepository;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.ReservationService;

@SpringBootTest
class ReservationExpiryJobIntegrationTest {

  private static final int INITIAL_STOCK = 30;

  @Autowired
  private ReservationExpiryJob reservationExpiryJob;

  @Autowired
  private ReservationService reservationService;

  @Autowired
  private TicketTypeRepository ticketTypeRepository;

  @Autowired
  private MatchRepository matchRepository;

  @Autowired
  private ReservationRepository reservationRepository;

  private Long matchId;
  private Long ticketTypeId;

  @BeforeEach
  void setUp() {
    MatchEntity match = new MatchEntity();
    match.setCompetition("Expiry Test League");
    match.setStage("Group Stage");
    match.setSeason("2026");
    match.setHomeTeam("Team A");
    match.setAwayTeam("Team B");
    match.setKickoffAt(LocalDateTime.now().plusDays(1));
    match.setStadium("Test Stadium");
    match.setStatus(0);
    match.setCreatedAt(LocalDateTime.now());
    match.setUpdatedAt(LocalDateTime.now());
    match = matchRepository.save(match);
    matchId = match.getId();

    TicketTypeEntity ticketType = new TicketTypeEntity();
    ticketType.setMatch(match);
    ticketType.setName("Expiry Test Ticket");
    ticketType.setDescription("Seeded for expiry-job test");
    ticketType.setPrice(100);
    ticketType.setQuantity(INITIAL_STOCK);
    ticketType.setAvailableQuantity(INITIAL_STOCK);
    ticketType = ticketTypeRepository.save(ticketType);
    ticketTypeId = ticketType.getId();
  }

  @AfterEach
  void tearDown() {
    reservationRepository.findAll().stream()
        .filter(reservation -> reservation.getTicketTypeId().equals(ticketTypeId))
        .forEach(reservation -> reservationRepository.deleteById(reservation.getId()));
    ticketTypeRepository.deleteById(ticketTypeId);
    matchRepository.deleteById(matchId);
  }

  private void expireImmediately(Long reservationId) {
    ReservationEntity reservation = reservationRepository.findById(reservationId).orElseThrow();
    reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    reservationRepository.save(reservation);
  }

  @Test
  void expireStaleReservations_flipsExpiredPendingToExpired_andReleasesStock() {
    ReservationDTO created = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 5));
    expireImmediately(created.getId());

    int expiredCount = reservationExpiryJob.expireStaleReservations();

    assertThat(expiredCount).isEqualTo(1);
    ReservationEntity reloaded = reservationRepository.findById(created.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ReservationStatusEnum.EXPIRED.toInt());
    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId)).isEqualTo(INITIAL_STOCK);
  }

  @Test
  void expireStaleReservations_leavesNotYetExpiredPendingReservationUntouched() {
    ReservationDTO created = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 5));

    int expiredCount = reservationExpiryJob.expireStaleReservations();

    assertThat(expiredCount).isEqualTo(0);
    ReservationEntity reloaded = reservationRepository.findById(created.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ReservationStatusEnum.PENDING.toInt());
    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId)).isEqualTo(INITIAL_STOCK - 5);
  }

  @Test
  void expireStaleReservations_leavesNonPendingReservationUntouched_evenIfExpiresAtIsInThePast() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setUserId(1L);
    reservation.setTicketTypeId(ticketTypeId);
    reservation.setStatus(ReservationStatusEnum.CANCELLED.toInt());
    reservation.setQuantity(5);
    reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    reservation = reservationRepository.save(reservation);

    int expiredCount = reservationExpiryJob.expireStaleReservations();

    assertThat(expiredCount).isEqualTo(0);
    ReservationEntity reloaded = reservationRepository.findById(reservation.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ReservationStatusEnum.CANCELLED.toInt());
    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId)).isEqualTo(INITIAL_STOCK);
  }

  @Test
  void expireStaleReservations_unblocksReReservation_afterHoldLapses() {
    ReservationDTO first = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 5));
    expireImmediately(first.getId());

    reservationExpiryJob.expireStaleReservations();

    ReservationDTO second = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 3));

    assertThat(second.getId()).isNotEqualTo(first.getId());
    assertThat(second.getStatus()).isEqualTo("PENDING");

    long persistedReservations = reservationRepository.findAll().stream()
        .filter(reservation -> reservation.getTicketTypeId().equals(ticketTypeId))
        .count();
    assertThat(persistedReservations).isEqualTo(2);
    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId)).isEqualTo(INITIAL_STOCK - 3);
  }
}
