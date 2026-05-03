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
@Component
public class MissedConnectionEligibilityStrategy implements EligibilityStrategy{
    private static final int MIN_DELAY_MINUTES = 180;

    @Override
    public IssueType supportedType() {
        return IssueType.MISSED_CONNECTION;
    }

    @Override
    public EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents) {
        boolean inScope = Boolean.TRUE.equals(euContext.getDepartureFromEu())||
                Boolean.TRUE.equals(euContext.getEuCarrier());

        boolean extraordinary = Boolean.TRUE.equals(issue.getExtraordinaryCircumstances());

        boolean delayEligible = issue.getType() == IssueType.MISSED_CONNECTION
                && issue.getDelayMinutes() != null
                && issue.getDelayMinutes() >= MIN_DELAY_MINUTES;

        boolean eligible = inScope && !extraordinary && (delayEligible);

        EligibilityResult result = new EligibilityResult();
        result.setEligible(eligible);
        result.setRequiredDocuments(List.of(DocumentTypes.TICKET, DocumentTypes.BOARDING_PASS));
        result.setCompensationAmount(eligible ? calculateCompensationAmount(flight.getDistanceKm()) : 0);
        return result;
    }

    public int calculateCompensationAmount(Integer distanceKm) {
        // Simplified compensation table by distance.
        if(distanceKm == null) return 0;
        if(distanceKm <= 1500) return 250;
        if(distanceKm <= 3500) return 400;
        return 600;
    }
}
