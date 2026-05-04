package com.claims.mvp.notifications;

import com.claims.mvp.claim.model.Claim;

public interface NotificationService {
    void sendClaimCreated(Claim claim);

    void sendClaimSubmitted(Claim claim);
}
