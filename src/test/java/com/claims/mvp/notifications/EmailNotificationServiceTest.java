package com.claims.mvp.notifications;

import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailNotificationServiceTest {

    @Test
    void sendClaimCreated_whenMailSenderThrows_doesNotPropagate() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        EmailNotificationService service = new EmailNotificationService(mailSender);
        ReflectionTestUtils.setField(service, "from", "no-reply@claims-mvp.local");

        Claim claim = new Claim();
        claim.setId(123L);

        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        claim.setUser(user);

        Flight flight = new Flight();
        flight.setAirline("Lufthansa");
        claim.setFlight(flight);

        assertThatCode(() -> service.sendClaimCreated(claim))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
