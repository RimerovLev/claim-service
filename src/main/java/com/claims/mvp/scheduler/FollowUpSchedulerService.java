package com.claims.mvp.scheduler;

import com.claims.mvp.claim.dto.request.StatusChangeRequest;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.events.dao.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Auto-promotes SUBMITTED claims to FOLLOW_UP_SENT after a delay window.
 *
 * Runs as a background cron job — there is no HTTP request and therefore
 * no SecurityContext. {@code claimService.transition} is protected by
 * {@code @PreAuthorize}, so we install a synthetic system principal with
 * ROLE_ADMIN before the loop and clear it afterwards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpSchedulerService {

    private static final String SYSTEM_PRINCIPAL = "system@scheduler";

    private final EventsRepository eventsRepository;
    private final ClaimService claimService;

    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void checkForFollowUps() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(14);
        List<Long> claimIds = eventsRepository.findClaimIdsEligibleForFollowUp(threshold);
        log.info("Found {} claims eligible for follow-up", claimIds.size());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        SYSTEM_PRINCIPAL, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
        try {
            for (Long claimId : claimIds) {
                try {
                    claimService.transition(
                            claimId,
                            new StatusChangeRequest(ClaimStatus.FOLLOW_UP_SENT, "auto follow-up")
                    );
                } catch (Exception e) {
                    log.error("Failed to send follow-up for claim {}", claimId, e);
                }
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
