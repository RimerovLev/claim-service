package com.claims.mvp.notifications;

import com.claims.mvp.claim.model.Claim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

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