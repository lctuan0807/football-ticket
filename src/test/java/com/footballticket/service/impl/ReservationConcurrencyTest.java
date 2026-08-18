package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.exceptions.InvalidReservationStateException;
import com.footballticket.exceptions.ReservationCreationFailedException;
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
}
