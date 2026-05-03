package com.claims.mvp.claim.service.letter.strategy;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.Claim;

/**
 * Letter generation per claim type.
 *
 * One strategy per {@link IssueType}. The orchestrator
 * {@code ClaimLetterServiceImpl} validates claim preconditions and
 * dispatches to the right strategy.
 *
 * Each strategy owns its own subject and body template, including the
 * legal basis it cites (e.g. EU 261/2004 vs Montreal Convention).
 */

public interface LetterStrategy {
    IssueType supportedIssueType();

    LetterResponse generateLetter(Claim claim);
}
