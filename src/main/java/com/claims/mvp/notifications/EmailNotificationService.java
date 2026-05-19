package com.claims.mvp.notifications;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.EventTypes;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.service.letter.ClaimLetterService;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.notifications.events.ClaimCreatedEvent;
import com.claims.mvp.notifications.events.ClaimStatusTransitionedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Sends emails in reaction to claim lifecycle events.
 * <p>
 * Subscribes to {@link ClaimCreatedEvent} and {@link ClaimStatusTransitionedEvent}
 * with {@link TransactionPhase#AFTER_COMMIT} — so notifications fire only after
 * the originating transaction has committed successfully. This avoids the
 * "email sent but claim not persisted" inconsistency.
 * <p>
 * Transition-time notifications are dispatched through the
 * {@link #transitionHandlers} map. To add a new notification for a status
 * (e.g. APPROVED), add one entry — no changes needed elsewhere.
 */

@Slf4j
@Service

public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;
    private final String from;
    private final Map<ClaimStatus, Consumer<Claim>> transitionHandlers;
    private final ClaimLetterService letterService;
    private final EventsRepository eventsRepository;

    public EmailNotificationService(JavaMailSender mailSender,
                                    @Value("${app.mail.from}") String from, ClaimLetterService letterService, EventsRepository eventsRepository) {
        this.mailSender = mailSender;
        this.from = from;
        this.letterService = letterService;
        this.eventsRepository = eventsRepository;
        this.transitionHandlers = Map.of(
                ClaimStatus.SUBMITTED,
                claim -> {
                    sendClaimLetterToAirline(claim);
                    sendClaimSubmitted(claim);
                },
                ClaimStatus.FOLLOW_UP_SENT, claim -> {
                    sendClaimLetterToAirline(claim);
                    sendClaimFollowUp(claim);
                }
                // future: ClaimStatus.APPROVED, this::sendClaimApproved
                //         ClaimStatus.REJECTED, this::sendClaimRejected
                //         ClaimStatus.PAID,     this::sendClaimPaid
        );
    }

    private static final Map<String, String> AIRLINE_EMAILS = Map.ofEntries(
            // Germany
            Map.entry("Lufthansa", "customer.relations@lufthansa.com"),
            Map.entry("Eurowings", "customerrelations@eurowings.com"),
            Map.entry("Condor", "customer-relations@condor.com"),

            // France
            Map.entry("Air France", "mail.internetsales.in@airfrance.fr"),
            Map.entry("Transavia", "customer.relations@transavia.com")
    );

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClaimCreated(ClaimCreatedEvent event) {
        sendClaimCreated(event.claim());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClaimTransitioned(ClaimStatusTransitionedEvent event) {
        Consumer<Claim> handler = transitionHandlers.get(event.to());
        if (handler != null) {
            handler.accept(event.claim());
        }
    }

    @Override
    public void sendClaimCreated(Claim claim) {
        send(
                claim.getUser().getEmail(),
                "Your claim has been received — #" + claim.getId(),
                """
                        Hello %s,

                        We have received your compensation claim (#%d).
                        Our team will review the details and get back to you shortly.

                        You can track the status of your claim in your account.
                        """.formatted(claim.getUser().getFullName(), claim.getId())
        );
    }

    @Override
    public void sendClaimSubmitted(Claim claim) {
        send(
                claim.getUser().getEmail(),
                "Your claim has been submitted to " + claim.getFlight().getAirline(),
                """
                        Hello %s,

                        Your compensation claim (#%d) has been submitted to %s.
                        We will follow up if there is no response within the standard window.
                        """.formatted(
                        claim.getUser().getFullName(),
                        claim.getId(),
                        claim.getFlight().getAirline()
                )
        );
    }

    public void sendClaimFollowUp(Claim claim) {
        send(
                claim.getUser().getEmail(),
                "Your claim has been follow-up to " + claim.getFlight().getAirline(),
                """
                        Hello %s,

                        Your compensation claim (#%d) has been follow-up to %s.
                        We will follow up if there is no response within the standard window.
                        """.formatted(
                        claim.getUser().getFullName(),
                        claim.getId(),
                        claim.getFlight().getAirline()
                )
        );
    }

    @Override
    public void sendClaimLetterToAirline(Claim claim) {
        LetterResponse letter = letterService.generateLetter(claim);
        String airlineEmail = AIRLINE_EMAILS.getOrDefault(claim.getFlight().getAirline(), "default@airline.com");
        send(airlineEmail, letter.getSubject(), letter.getBody());
        recordEmailSent(claim, airlineEmail, letter.getSubject());
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            // Email failure must not break the main flow.
            log.error("Failed to send email to {}: {}", to, subject, e);
        }

    }

    private void recordEmailSent(Claim claim, String airlineEmail, String subject){
        ClaimEvents event = new ClaimEvents();
        event.setClaim(claim);
        event.setType(
                EventTypes.EMAIL_SENT
        );
        event.setPayload("{\"to\":\"%s\",\"subject\":\"%s\"}".formatted(airlineEmail, subject));
        eventsRepository.save(event);
        log.info("EMAIL_SENT recorded for claim #{}: to={}", claim.getId(), airlineEmail);
    }

}

