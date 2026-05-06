package com.claims.mvp.notifications.events;

import com.claims.mvp.claim.model.Claim;

/**
 * Published after a new claim has been created and persisted.
 * Listeners must use {@code @TransactionalEventListener(AFTER_COMMIT)} to ensure
 * notifications fire only after the creating transaction successfully commits.
 */

public record ClaimCreatedEvent(Claim claim) {
}
