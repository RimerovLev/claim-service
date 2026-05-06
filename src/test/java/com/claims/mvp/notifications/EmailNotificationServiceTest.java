package com.claims.mvp.notifications;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.notifications.events.ClaimCreatedEvent;
import com.claims.mvp.notifications.events.ClaimStatusTransitionedEvent;
import com.claims.mvp.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNotificationServiceTest {

    private JavaMailSender mailSender;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new EmailNotificationService(mailSender, "no-reply@claims-mvp.local");
    }

    @Test
    void sendClaimCreated_whenMailSenderThrows_doesNotPropagate() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.sendClaimCreated(claim(123L)))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void onClaimCreated_delegatesToSendClaimCreated() {
        service.onClaimCreated(new ClaimCreatedEvent(claim(42L)));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void onClaimTransitioned_toSubmitted_sendsEmail() {
        Claim c = claim(7L);
        service.onClaimTransitioned(
                new ClaimStatusTransitionedEvent(c, ClaimStatus.READY_TO_SUBMIT, ClaimStatus.SUBMITTED)
        );
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void onClaimTransitioned_toFollowUpSent_doesNotSendEmail() {
        Claim c = claim(7L);
        service.onClaimTransitioned(
                new ClaimStatusTransitionedEvent(c, ClaimStatus.SUBMITTED, ClaimStatus.FOLLOW_UP_SENT)
        );
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    private Claim claim(Long id) {
        Claim claim = new Claim();
        claim.setId(id);

        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        claim.setUser(user);

        Flight flight = new Flight();
        flight.setAirline("Lufthansa");
        claim.setFlight(flight);

        return claim;
    }
}
