package com.claims.mvp.notifications;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.service.letter.ClaimLetterService;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.notifications.events.ClaimCreatedEvent;
import com.claims.mvp.notifications.events.ClaimStatusTransitionedEvent;
import com.claims.mvp.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;


import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNotificationServiceTest {

    private JavaMailSender mailSender;
    private EmailNotificationService service;
    private EventsRepository eventsRepository;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        ClaimLetterService letterService = mock(ClaimLetterService.class);

        // Mock the letter generation
        LetterResponse mockLetter = new LetterResponse(
                "Test Subject",
                "Test Body"
        );
        when(letterService.generateLetter(any(Claim.class)))
                .thenReturn(mockLetter);

        service = new EmailNotificationService(mailSender, "no-reply@claims-mvp.local", letterService, eventsRepository);
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
        // Now expects 2 sends: one to user, one to airline
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }


    @Test
    void onClaimTransitioned_toFollowUpSent_doesNotSendEmail() {
        Claim c = claim(7L);
        service.onClaimTransitioned(
                new ClaimStatusTransitionedEvent(c, ClaimStatus.SUBMITTED, ClaimStatus.FOLLOW_UP_SENT)
        );
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void onClaimTransitioned_toSubmitted_sendsEmailTwice() {
        Claim c = claim(7L);

        service.onClaimTransitioned(
                new ClaimStatusTransitionedEvent(c, ClaimStatus.READY_TO_SUBMIT, ClaimStatus.SUBMITTED)
        );

        // Verify: mailSender.send called twice (once to user, once to airline)
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void onClaimTransitioned_toSubmitted_sendsToUserAndAirline() {
        Claim c = claim(7L);

        service.onClaimTransitioned(
                new ClaimStatusTransitionedEvent(c, ClaimStatus.READY_TO_SUBMIT, ClaimStatus.SUBMITTED)
        );

        // Verify messages captured
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());

        List<SimpleMailMessage> messages = captor.getAllValues();
        assertThat(messages).hasSize(2);

        // First: to airline
        assertThat(messages.get(0).getTo()[0]).isEqualTo("customer.relations@lufthansa.com");
        assertThat(messages.get(0).getSubject()).isNotBlank();

// Second: to user
        assertThat(messages.get(1).getTo()[0]).isEqualTo("test@example.com");
        assertThat(messages.get(1).getSubject()).contains("submitted to");

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
