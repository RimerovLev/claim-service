package com.claims.mvp.eligibility.service;

import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.eligibility.dto.response.EligibilityResult;
import com.claims.mvp.eligibility.strategy.EligibilityStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * EligibilityService.
 *
 * Pure business logic (no DB access). Determines:
 * - eligible (does the claim qualify under the rules)
 * - compensationAmount (how much to pay)
 * - requiredDocuments (which documents must be collected from the customer)
 *
 * Delegates per-IssueType rules to {@link EligibilityStrategy} implementations.
 * To support a new claim type, add a new strategy bean — no changes here.
 */
@Service

public class EligibilityServiceImpl implements EligibilityService{

    private final Map<IssueType, EligibilityStrategy> strategiesByType;
    public EligibilityServiceImpl(List<EligibilityStrategy> strategies) {
        this.strategiesByType = strategies.stream()
                .collect(Collectors.toMap (EligibilityStrategy::supportedType,
                        Function.identity()
                ));
    }

    @Override
    public EligibilityResult evaluate(Issue issue,
                                      Flight flight,
                                      EuContext euContext,
                                      List<BoardingDocuments> documents) {
        if(issue == null || issue.getType() == null) {
            throw new IllegalArgumentException("Issue type must be set");
        }
        EligibilityStrategy strategy = strategiesByType.get(issue.getType());
        if(strategy == null) {
            throw new IllegalArgumentException("No eligibility strategy for type : " + issue.getType());
        }

       return strategy.evaluate(issue, flight, euContext, documents);
    }

    @Override
    public int calculateCompensationAmount(Integer distanceKm) {
        // Kept for backward compatibility with the EligibilityService interface.
        // The actual amount calculation lives in each strategy.
        if (distanceKm == null) return 0;
        if (distanceKm <= 1500) return 250;
        if (distanceKm <= 3500) return 400;
        return 600;
    }

}
