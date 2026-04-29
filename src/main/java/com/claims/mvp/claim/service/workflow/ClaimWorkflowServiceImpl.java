package com.claims.mvp.claim.service.workflow;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.EventTypes;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
/**
 * ClaimWorkflowService.
 *
 * Owns claim status workflow rules:
 * - which transitions are allowed (FSM)
 * - auto-switch between DOCS_REQUESTED and READY_TO_SUBMIT in the "pre-submit" stage
 *
 * Important: after SUBMITTED we do not auto-change the status anymore — only manual transitions.
 */
@Service

public class ClaimWorkflowServiceImpl implements ClaimWorkflowService {

    /**
     * Allowed status transitions (FSM):
     * <p>
     * Lifecycle:
     * NEW → DOCS_REQUESTED (documents required)
     * → READY_TO_SUBMIT (eligible, no docs needed)
     * DOCS_REQUESTED → READY_TO_SUBMIT
     * READY_TO_SUBMIT → SUBMITTED
     * SUBMITTED → FOLLOW_UP_SENT, APPROVED, REJECTED
     * FOLLOW_UP_SENT → APPROVED, REJECTED
     * APPROVED → PAID
     * REJECTED → CLOSED
     * PAID → CLOSED
     * CLOSED → (terminal)
     */

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED_TRANSITIONS = Map.of(
            ClaimStatus.NEW, Set.of(ClaimStatus.DOCS_REQUESTED, ClaimStatus.READY_TO_SUBMIT),
            ClaimStatus.DOCS_REQUESTED, Set.of(ClaimStatus.READY_TO_SUBMIT),
            ClaimStatus.READY_TO_SUBMIT, Set.of(ClaimStatus.SUBMITTED),
            ClaimStatus.SUBMITTED, Set.of(ClaimStatus.FOLLOW_UP_SENT, ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.FOLLOW_UP_SENT, Set.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.APPROVED, Set.of(ClaimStatus.PAID),
            ClaimStatus.REJECTED, Set.of(ClaimStatus.CLOSED),
            ClaimStatus.PAID, Set.of(ClaimStatus.CLOSED)
    );

    private static final Map<ClaimStatus, EventTypes> EVENT_BY_TARGET = new EnumMap<>(Map.of(
            ClaimStatus.SUBMITTED, EventTypes.LETTER_SUBMITTED,
            ClaimStatus.FOLLOW_UP_SENT, EventTypes.FOLLOW_UP_SENT,
            ClaimStatus.APPROVED, EventTypes.CLAIM_APPROVED,
            ClaimStatus.REJECTED, EventTypes.CLAIM_REJECTED,
            ClaimStatus.PAID, EventTypes.CLAIM_PAID,
            ClaimStatus.CLOSED, EventTypes.CLAIM_CLOSED
    ));

    @Override
    public void assertTransitionAllowed(ClaimStatus from, ClaimStatus to) {
        // Used by the manual status change endpoint (/status).
        if (from == null || to == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("Transition from " + from + " to " + to + " is not allowed");
        }
    }

    @Override
    public ClaimStatus autoPreSubmitStatus(ClaimStatus current, boolean hasAllRequiredDocuments) {
        // Auto-status only in the pre-submit stage:
        // - if all required documents are uploaded -> READY_TO_SUBMIT
        // - otherwise -> DOCS_REQUESTED
        // After SUBMITTED (and beyond) we do not interfere.
        if (current == ClaimStatus.NEW
                || current == ClaimStatus.DOCS_REQUESTED
                || current == ClaimStatus.READY_TO_SUBMIT) {
            return hasAllRequiredDocuments ? ClaimStatus.READY_TO_SUBMIT : ClaimStatus.DOCS_REQUESTED;
        }
        return current;
    }

    @Override
    public void assertEditable(ClaimStatus current) {
        // Only allow editing in the pre-submit stage (NEW, DOCS_REQUESTED, READY_TO_SUBMIT).
        if (current == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        if (current != ClaimStatus.NEW
                && current != ClaimStatus.DOCS_REQUESTED
                && current != ClaimStatus.READY_TO_SUBMIT) {
            throw new IllegalStateException("Claim with status " + current + " is not editable");
        }
    }

    @Override
    public EventTypes eventType(ClaimStatus targetStatus) {
        if(targetStatus == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        return EVENT_BY_TARGET.getOrDefault(targetStatus, EventTypes.STATUS_CHANGED);
    }


}
