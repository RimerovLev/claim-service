package com.claims.mvp.notifications;

import com.claims.mvp.claim.dto.request.StatusChangeRequest;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.scheduler.FollowUpSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpSchedulerServiceTest {

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private ClaimService claimService;

    private FollowUpSchedulerService service;

    @BeforeEach
    void setUp() {
        service = new FollowUpSchedulerService(eventsRepository, claimService);
    }

    @Test
    void checkForFollowUps_withEligibleClaims_transitionsEach() {
        when(eventsRepository.findClaimIdsEligibleForFollowUp(any())).thenReturn(List.of(1L, 2L, 3L));

        service.checkForFollowUps();

        verify(claimService, times(3)).transition(anyLong(), any(StatusChangeRequest.class));
    }

    @Test
    void checkForFollowUps_noEligibleClaims_doesNotTransition() {
        when(eventsRepository.findClaimIdsEligibleForFollowUp(any())).thenReturn(List.of());

        service.checkForFollowUps();

        verify(claimService, never()).transition(anyLong(), any(StatusChangeRequest.class));
    }

    @Test
    void checkForFollowUps_transitionsToFollowUpSent() {
        when(eventsRepository.findClaimIdsEligibleForFollowUp(any())).thenReturn(List.of(7L));

        service.checkForFollowUps();

        ArgumentCaptor<StatusChangeRequest> captor = ArgumentCaptor.forClass(StatusChangeRequest.class);
        verify(claimService).transition(eq(7L), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClaimStatus.FOLLOW_UP_SENT);
    }

    @Test
    void checkForFollowUps_oneClaimFails_continuesWithRest() {
        when(eventsRepository.findClaimIdsEligibleForFollowUp(any())).thenReturn(List.of(1L, 2L, 3L));
        doThrow(new RuntimeException("transition failed")).when(claimService).transition(eq(2L), any());

        assertThatCode(() -> service.checkForFollowUps()).doesNotThrowAnyException();

        verify(claimService, times(3)).transition(anyLong(), any(StatusChangeRequest.class));
    }
}