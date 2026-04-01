package com.claims.mvp.claim.service.workflow;

import com.claims.mvp.claim.enums.ClaimStatus;

public interface ClaimWorkflowService {
    void assertTransitionAllowed(ClaimStatus from, ClaimStatus to);

    ClaimStatus autoPreSubmitStatus(ClaimStatus current, boolean hasAllRequiredDocuments);
}

