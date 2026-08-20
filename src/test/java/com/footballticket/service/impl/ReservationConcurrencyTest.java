package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.exceptions.InvalidReservationStateException;
import com.footballticket.exceptions.ReservationCreationFailedException;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.repository.MatchRepository;
import com.footballticket.repository.ReservationRepository;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.ReservationService;

@SpringBootTest
class ReservationConcurrencyTest {

  private static final int INITIAL_STOCK = 30;
  private static final int CONCURRENT_REQUESTS = 150;

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
    match.setCompetition("Concurrency Test League");
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
    ticketType.setName("Concurrency Test Ticket");
    ticketType.setDescription("Seeded for race-condition test");
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

  @Test
  void createReservation_neverOversellsStock_underConcurrentRequests() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(30);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_REQUESTS);
    AtomicInteger successCount = new AtomicInteger();

    for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();
          ReservationDTO result = reservationService.createReservation(matchId, ticketTypeId,
              new CreateReservationRequest(1L, 1));
          if (result != null) {
            successCount.incrementAndGet();
          }
        } catch (InsufficientTicketException e) {
          // expected once stock is exhausted
        } catch (ReservationCreationFailedException e) {
          // expected once the atomic decrement loses the race after the pre-check passed
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(completed).as("all requests finished before timeout").isTrue();

    int remainingStock = ticketTypeRepository.getAvailableQuantity(ticketTypeId);

    assertThat(remainingStock).as("stock must never go negative").isGreaterThanOrEqualTo(0);
    assertThat(successCount.get()).as("successful reservations must not exceed initial stock")
        .isLessThanOrEqualTo(INITIAL_STOCK);
    assertThat(successCount.get()).as("successful reservations must exactly account for consumed stock")
        .isEqualTo(INITIAL_STOCK - remainingStock);

    long persistedReservations = reservationRepository.findAll().stream()
        .filter(reservation -> reservation.getTicketTypeId().equals(ticketTypeId))
        .count();
    assertThat(persistedReservations).as("a reservation record must be persisted for each successful call")
        .isEqualTo(successCount.get());
  }

  @Test
  void createReservation_throwsResourceAlreadyExistsException_whenUserAlreadyHasPendingReservationForTicketType() {
    reservationService.createReservation(matchId, ticketTypeId, new CreateReservationRequest(1L, 2));

    assertThatThrownBy(
        () -> reservationService.createReservation(matchId, ticketTypeId, new CreateReservationRequest(1L, 3)))
        .isInstanceOf(ResourceAlreadyExistsException.class);

    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId)).isEqualTo(INITIAL_STOCK - 2);

    long persistedReservations = reservationRepository.findAll().stream()
        .filter(reservation -> reservation.getTicketTypeId().equals(ticketTypeId))
        .count();
    assertThat(persistedReservations).as("the rejected duplicate must not be persisted").isEqualTo(1);
  }

  @Test
  void createReservation_allowsSecondPendingReservation_whenDifferentUsersRequestSameTicketType() {
    ReservationDTO first = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 2));
    ReservationDTO second = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(2L, 2));

    assertThat(first.getId()).isNotEqualTo(second.getId());
    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId)).isEqualTo(INITIAL_STOCK - 4);
  }

  @Test
  void cancelReservation_neverDoubleReleasesStock_underConcurrentCancelRequests() throws InterruptedException {
    ReservationDTO created = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 5));
    int stockAfterCreate = ticketTypeRepository.getAvailableQuantity(ticketTypeId);

    int concurrentCancelAttempts = 10;
    ExecutorService executor = Executors.newFixedThreadPool(concurrentCancelAttempts);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(concurrentCancelAttempts);
    AtomicInteger successCount = new AtomicInteger();

    for (int i = 0; i < concurrentCancelAttempts; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();
          reservationService.cancelReservation(created.getId());
          successCount.incrementAndGet();
        } catch (InvalidReservationStateException e) {
          // expected for every loser of the race
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(completed).as("all requests finished before timeout").isTrue();
    assertThat(successCount.get()).as("exactly one concurrent cancel should win").isEqualTo(1);
    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId))
        .as("stock must be released exactly once, not once per attempt")
        .isEqualTo(stockAfterCreate + 5);
  }

  @Test
  void confirmAndCancelReservation_exactlyOneWins_whenRacedConcurrentlyOnSamePendingReservation()
      throws InterruptedException {
    ReservationDTO created = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 5));
    int stockAfterCreate = ticketTypeRepository.getAvailableQuantity(ticketTypeId);

    int concurrentAttempts = 10;
    ExecutorService executor = Executors.newFixedThreadPool(concurrentAttempts);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(concurrentAttempts);
    AtomicInteger confirmSuccessCount = new AtomicInteger();
    AtomicInteger cancelSuccessCount = new AtomicInteger();

    for (int i = 0; i < concurrentAttempts; i++) {
      boolean confirmBranch = i % 2 == 0;
      executor.submit(() -> {
        try {
          startLatch.await();
          if (confirmBranch) {
            reservationService.confirmReservation(created.getId());
            confirmSuccessCount.incrementAndGet();
          } else {
            reservationService.cancelReservation(created.getId());
            cancelSuccessCount.incrementAndGet();
          }
        } catch (InvalidReservationStateException e) {
          // expected for every loser of the race
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(completed).as("all requests finished before timeout").isTrue();

    int totalSuccesses = confirmSuccessCount.get() + cancelSuccessCount.get();
    assertThat(totalSuccesses).as("exactly one of confirm/cancel should win the race").isEqualTo(1);

    int finalStock = ticketTypeRepository.getAvailableQuantity(ticketTypeId);
    if (cancelSuccessCount.get() == 1) {
      assertThat(finalStock).as("stock must be released back when cancel wins").isEqualTo(stockAfterCreate + 5);
    } else {
      assertThat(finalStock).as("stock must remain decremented when confirm wins").isEqualTo(stockAfterCreate);
    }
  }

  @Test
  void confirmReservation_lazilyExpiresAndReleasesStockInDatabase_whenHoldExpired() {
    ReservationDTO created = reservationService.createReservation(matchId, ticketTypeId,
        new CreateReservationRequest(1L, 4));
    int stockAfterCreate = ticketTypeRepository.getAvailableQuantity(ticketTypeId);

    ReservationEntity reservation = reservationRepository.findById(created.getId()).orElseThrow();
    reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    reservationRepository.save(reservation);

    assertThatThrownBy(() -> reservationService.confirmReservation(created.getId()))
        .isInstanceOf(InvalidReservationStateException.class);

    ReservationEntity afterAttempt = reservationRepository.findById(created.getId()).orElseThrow();
    assertThat(afterAttempt.getStatus()).as("reservation must actually persist as EXPIRED, not roll back")
        .isEqualTo(ReservationStatusEnum.EXPIRED.toInt());
    assertThat(ticketTypeRepository.getAvailableQuantity(ticketTypeId))
        .as("stock must actually be released back to the database, not rolled back")
        .isEqualTo(stockAfterCreate + 4);
  }
}
