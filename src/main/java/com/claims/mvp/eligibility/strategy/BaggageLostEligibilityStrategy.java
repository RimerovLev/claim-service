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
public class BaggageLostEligibilityStrategy implements EligibilityStrategy {

    // Montreal Convention: baggage is officially "lost" after 21 days without return.
    // 21 days * 24 hours = 504 hours. Condition is strictly greater than.
    private static final int LOST_THRESHOLD_HOURS = 504;
    private static final int COMPENSATION_AMOUNT = 1000;

    @Override
    public IssueType supportedType() {
        return IssueType.BAGGAGE_LOST;
    }

    @Override
    public EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents) {
        boolean extraordinary = Boolean.TRUE.equals(issue.getExtraordinaryCircumstances());
        boolean lostThresholdMet = issue.getBaggageDelayHours() != null
                && issue.getBaggageDelayHours() > LOST_THRESHOLD_HOURS;

        boolean eligible = !extraordinary && lostThresholdMet;

        EligibilityResult result = new EligibilityResult();
        result.setEligible(eligible);
        result.setCompensationAmount(eligible ? COMPENSATION_AMOUNT : 0);
        result.setRequiredDocuments(List.of(DocumentTypes.BAG_TAG, DocumentTypes.PIR));
        return result;
    }
}
