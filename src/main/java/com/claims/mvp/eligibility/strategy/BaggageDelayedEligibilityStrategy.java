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
public class BaggageDelayedEligibilityStrategy implements EligibilityStrategy{
    private static final int PER_DAY_AMOUNT = 50;
    private static final int MAX_DAYS = 30;
    private static final int MIN_DELAY_HOURS = 6;

    @Override
    public IssueType supportedType() {
        return IssueType.BAGGAGE_DELAYED;
    }

    @Override
    public EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents) {
        boolean extraordinary = Boolean.TRUE.equals(issue.getExtraordinaryCircumstances());
        boolean delayEligible = issue.getBaggageDelayHours() != null
                && issue.getBaggageDelayHours() >= MIN_DELAY_HOURS;
        boolean eligible = !extraordinary && (delayEligible);
        EligibilityResult result = new EligibilityResult();
        result.setEligible(eligible);
        result.setRequiredDocuments(List.of(DocumentTypes.BAG_TAG, DocumentTypes.PIR));
        result.setCompensationAmount(eligible ? calculateCompensationAmount(issue.getBaggageDelayHours()) : 0);
        return result;
    }

    private int calculateCompensationAmount(int delayHours) {
        int days = (int) Math.ceil(delayHours / 24.0);
        return Math.min(days, MAX_DAYS) * PER_DAY_AMOUNT;
    }
}
