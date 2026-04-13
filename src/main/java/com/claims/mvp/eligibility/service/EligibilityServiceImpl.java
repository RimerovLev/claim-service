package com.claims.mvp.eligibility.service;

import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.eligibility.dto.response.EligibilityResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * EligibilityService.
 *
 * Pure business logic (no DB access). Determines:
 * - eligible (does the claim qualify under the rules)
 * - compensationAmount (how much to pay)
 * - requiredDocuments (which documents must be collected from the customer)
 *
 * This service does not change claim status and does not persist anything — it only returns a result.
 */
public class EligibilityServiceImpl implements EligibilityService{

    @Override
    public EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents) {
        // Main evaluation method (eligibility + required docs + compensation).

        // 1) In scope (EU261):
        // If the flight departs from the EU OR the carrier is an EU carrier -> in scope.
        boolean inScope = Boolean.TRUE.equals(euContext.getDepartureFromEu())
                || Boolean.TRUE.equals(euContext.getEuCarrier());

        // 2) Extraordinary circumstances:
        // If extraordinary=true -> no compensation.
        boolean extraordinary = Boolean.TRUE.equals(issue.getExtraordinaryCircumstances());

        // 3) Delay eligibility:
        // - type=DELAY
        // - delayMinutes specified
        // - delay >= 180 minutes (3 hours)
        boolean delayEligible = issue.getType() == IssueType.DELAY
                && issue.getDelayMinutes() != null
                && issue.getDelayMinutes() >= 180;

        // 4) Cancellation eligibility:
        // - type=CANCELLATION
        // - cancellationNoticeDays specified
        // - notice <= 14 days before departure
        boolean cancelEligible = issue.getType() == IssueType.CANCELLATION
                && issue.getCancellationNoticeDays() != null
                && issue.getCancellationNoticeDays() <= 14;

        EligibilityResult result = new EligibilityResult();
        // Required docs rule: currently DELAY/CANCELLATION require Ticket+BoardingPass.
        // Other document sets are prepared for future issue types.
        boolean isFlightClaim = issue.getType() == IssueType.DELAY || issue.getType() == IssueType.CANCELLATION;
        result.setRequiredDocuments(
                isFlightClaim
                        ? List.of(DocumentTypes.TICKET, DocumentTypes.BOARDING_PASS)
                        : List.of(DocumentTypes.PIR, DocumentTypes.BAG_TAG, DocumentTypes.PHOTO)
        );

        boolean eligible = inScope && !extraordinary && (delayEligible || cancelEligible);
        result.setEligible(eligible);
        result.setCompensationAmount(eligible ? calculateCompensationAmount(flight.getDistanceKm()) : 0);

        return result;
    }

    @Override
    public int calculateCompensationAmount(Integer distanceKm) {
        // Simplified compensation table by distance.
        if(distanceKm == null) return 0;
        if(distanceKm <= 1500) return 250;
        if(distanceKm <= 3500) return 400;
        return 600;
    }

}
