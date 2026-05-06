package com.claims.mvp.notifications;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.notifications.events.ClaimCreatedEvent;
import com.claims.mvp.notifications.events.ClaimStatusTransitionedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
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

    public EmailNotificationService(JavaMailSender mailSender,
                                    @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
        this.transitionHandlers = Map.of(
                ClaimStatus.SUBMITTED, this::sendClaimSubmitted
                // future: ClaimStatus.APPROVED, this::sendClaimApproved
                //         ClaimStatus.REJECTED, this::sendClaimRejected
                //         ClaimStatus.PAID,     this::sendClaimPaid
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClaimCreated(ClaimCreatedEvent event) {
        sendClaimCreated(event.claim());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
}