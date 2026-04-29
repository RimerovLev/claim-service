package com.claims.mvp.claim.service.workflow;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.EventTypes;

public interface ClaimWorkflowService {
    void assertTransitionAllowed(ClaimStatus from, ClaimStatus to);

    ClaimStatus autoPreSubmitStatus(ClaimStatus current, boolean hasAllRequiredDocuments);

    void assertEditable(ClaimStatus current);

    EventTypes eventType(ClaimStatus targetStatus);
}

