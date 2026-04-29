package com.claims.mvp.eligibility.strategy;

import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.eligibility.dto.response.EligibilityResult;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * EU 261/2004 eligibility for flight cancellations.
 *
 * Eligible when:
 *  - flight is in scope (departs from EU OR carrier is EU);
 *  - cancellation is not caused by extraordinary circumstances;
 *  - passenger was notified at most 14 days before departure.
 *
 * Compensation amount uses the same distance table as delay claims:
 *  - up to 1500 km → 250 EUR
 *  - up to 3500 km → 400 EUR
 *  - over 3500 km  → 600 EUR
 *
 * Required documents: ticket and boarding pass.
 */
@Component
public class CancellationEligibilityStrategy implements EligibilityStrategy {
    public static final int MAX_NOTICE_DAYS = 14;

    @Override
    public IssueType supportedType() {
        return IssueType.CANCELLATION;
    }

    @Override
    public EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents) {
        boolean inScope = Boolean.TRUE.equals(euContext.getDepartureFromEu())
                || Boolean.TRUE.equals(euContext.getEuCarrier());


        boolean extraordinary = Boolean.TRUE.equals(issue.getExtraordinaryCircumstances());

        boolean cancelEligible = issue.getType() == IssueType.CANCELLATION
                && issue.getCancellationNoticeDays() != null
                && issue.getCancellationNoticeDays() <= MAX_NOTICE_DAYS;

        boolean eligible = inScope && !extraordinary && (cancelEligible);

        EligibilityResult result = new EligibilityResult();
        result.setEligible(eligible);
        result.setCompensationAmount(eligible ? calculateCompensationAmount(flight.getDistanceKm()) : 0);
        result.setRequiredDocuments(List.of(DocumentTypes.TICKET, DocumentTypes.BOARDING_PASS));

        return result;
    }


    public int calculateCompensationAmount(Integer distanceKm) {
        // Simplified compensation table by distance.
        if (distanceKm == null) return 0;
        if (distanceKm <= 1500) return 250;
        if (distanceKm <= 3500) return 400;
        return 600;
    }
}
