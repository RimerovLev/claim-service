package com.claims.mvp.notifications.events;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.model.Claim;

/**
 * Published after a claim's status has transitioned (e.g. NEW -> SUBMITTED).
 * Listeners can dispatch behaviour by inspecting {@link #to()}.
 * Use {@code @TransactionalEventListener(AFTER_COMMIT)} so that side effects
 * fire only after the transition is committed.
 */

public record ClaimStatusTransitionedEvent(Claim claim, ClaimStatus from, ClaimStatus to) {

}
