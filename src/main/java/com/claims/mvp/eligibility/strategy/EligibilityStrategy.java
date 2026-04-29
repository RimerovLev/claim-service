package com.claims.mvp.eligibility.strategy;

import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.eligibility.dto.response.EligibilityResult;

import java.util.List;
/**
 * Eligibility rule per claim type.
 *
 * One strategy per {@link IssueType}. The registry in
 * {@code EligibilityServiceImpl} dispatches to the right strategy based
 * on {@code Issue.type}.
 *
 * Strategies must remain pure: no DB access, no side effects — input
 * comes via arguments only.
 */

public interface EligibilityStrategy {
    IssueType supportedType();
    EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents);
}
