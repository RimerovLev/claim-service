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
public class BaggageDamagedEligibilityStrategy implements EligibilityStrategy{
    private static final int MAX_DAYS_SINCE = 7;
    private static final int COMPENSATION_AMOUNT = 800;
    @Override
    public IssueType supportedType() {
        return IssueType.BAGGAGE_DAMAGED;
    }

    @Override
    public EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents) {
        boolean extraordinary = Boolean.TRUE.equals(issue.getExtraordinaryCircumstances());

        boolean withinDeadline = issue.getDaysSinceDelivery() != null
                && issue.getDaysSinceDelivery() <= MAX_DAYS_SINCE;
        boolean eligible = !extraordinary && withinDeadline;

        EligibilityResult result = new EligibilityResult();
        result.setEligible(eligible);
        result.setCompensationAmount(eligible ? COMPENSATION_AMOUNT : 0);
        result.setRequiredDocuments(List.of(DocumentTypes.PIR, DocumentTypes.PHOTO));
        return result;
    }
}
