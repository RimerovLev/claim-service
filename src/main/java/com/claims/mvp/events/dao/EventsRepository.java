package com.claims.mvp.events.dao;

import com.claims.mvp.events.model.ClaimEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface EventsRepository extends JpaRepository<ClaimEvents, Long> {
    List<ClaimEvents> findByClaimIdOrderByCreatedAtDesc(Long claimId);

    @Query("""
            SELECT e.claim.id FROM ClaimEvents e
                        WHERE e.type = com.claims.mvp.claim.enums.EventTypes.LETTER_SUBMITTED
                                    AND e.createdAt <= :threshold
                                                AND e.claim.status = com.claims.mvp.claim.enums.ClaimStatus.SUBMITTED
            """)
    List<Long> findClaimIdsEligibleForFollowUp(OffsetDateTime threshold);
}
