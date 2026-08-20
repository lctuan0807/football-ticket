package com.footballticket.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.footballticket.entity.ReservationEntity;
import com.footballticket.enums.ReservationStatusEnum;
import com.footballticket.repository.ReservationRepository;
import com.footballticket.repository.TicketTypeRepository;

@ExtendWith(MockitoExtension.class)
class ReservationExpiryJobTest {

  private static final int BATCH_SIZE = 200;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private TicketTypeRepository ticketTypeRepository;

  private ReservationExpiryJob job;

  @BeforeEach
  void setUp() {
    job = new ReservationExpiryJob(reservationRepository, ticketTypeRepository);
    ReflectionTestUtils.setField(job, "batchSize", BATCH_SIZE);
  }

  private ReservationEntity reservation(Long id, Long userId, Long ticketTypeId, int quantity) {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(id);
    reservation.setUserId(userId);
    reservation.setTicketTypeId(ticketTypeId);
    reservation.setStatus(ReservationStatusEnum.PENDING.toInt());
    reservation.setQuantity(quantity);
    reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    return reservation;
  }

  @Test
  void expireStaleReservations_expiresAndReleasesStock_forBatchOfStalePendingReservations() {
    ReservationEntity first = reservation(100L, 1L, 10L, 2);
    ReservationEntity second = reservation(101L, 2L, 11L, 3);
    given(reservationRepository.findByStatusAndExpiresAtBefore(
        eq(ReservationStatusEnum.PENDING.toInt()), any(LocalDateTime.class), any(Pageable.class)))
        .willReturn(List.of(first, second));
    given(reservationRepository.updateStatusIfCurrentStatus(100L, ReservationStatusEnum.PENDING.toInt(),
        ReservationStatusEnum.EXPIRED.toInt())).willReturn(1);
    given(reservationRepository.updateStatusIfCurrentStatus(101L, ReservationStatusEnum.PENDING.toInt(),
        ReservationStatusEnum.EXPIRED.toInt())).willReturn(1);

    int expiredCount = job.expireStaleReservations();

    assertThat(expiredCount).isEqualTo(2);

    ArgumentCaptor<Long> ticketTypeIdCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(ticketTypeRepository, org.mockito.Mockito.times(2)).release(ticketTypeIdCaptor.capture(),
        quantityCaptor.capture());
    assertThat(ticketTypeIdCaptor.getAllValues()).containsExactly(10L, 11L);
    assertThat(quantityCaptor.getAllValues()).containsExactly(2, 3);

    verify(reservationRepository).updateStatusIfCurrentStatus(100L, ReservationStatusEnum.PENDING.toInt(),
        ReservationStatusEnum.EXPIRED.toInt());
    verify(reservationRepository).updateStatusIfCurrentStatus(101L, ReservationStatusEnum.PENDING.toInt(),
        ReservationStatusEnum.EXPIRED.toInt());
  }

  @Test
  void expireStaleReservations_skipsRelease_whenCasUpdateLosesRace() {
    ReservationEntity reservation = reservation(100L, 1L, 10L, 2);
    given(reservationRepository.findByStatusAndExpiresAtBefore(
        eq(ReservationStatusEnum.PENDING.toInt()), any(LocalDateTime.class), any(Pageable.class)))
        .willReturn(List.of(reservation));
    given(reservationRepository.updateStatusIfCurrentStatus(100L, ReservationStatusEnum.PENDING.toInt(),
        ReservationStatusEnum.EXPIRED.toInt())).willReturn(0);

    int expiredCount = job.expireStaleReservations();

    assertThat(expiredCount).isEqualTo(0);
    verify(ticketTypeRepository, never()).release(any(), anyInt());
  }

  @Test
  void expireStaleReservations_noOp_whenNothingExpired() {
    given(reservationRepository.findByStatusAndExpiresAtBefore(
        eq(ReservationStatusEnum.PENDING.toInt()), any(LocalDateTime.class), any(Pageable.class)))
        .willReturn(List.of());

    int expiredCount = job.expireStaleReservations();

    assertThat(expiredCount).isEqualTo(0);
    verify(reservationRepository, never()).updateStatusIfCurrentStatus(any(), any(), any());
    verify(ticketTypeRepository, never()).release(any(), anyInt());
  }

  @Test
  void expireStaleReservations_passesBoundedPageableToRepository() {
    given(reservationRepository.findByStatusAndExpiresAtBefore(
        eq(ReservationStatusEnum.PENDING.toInt()), any(LocalDateTime.class), any(Pageable.class)))
        .willReturn(List.of());

    job.expireStaleReservations();

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(reservationRepository).findByStatusAndExpiresAtBefore(eq(ReservationStatusEnum.PENDING.toInt()),
        any(LocalDateTime.class), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(BATCH_SIZE);
  }
}
