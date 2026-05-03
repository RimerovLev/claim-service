package com.claims.mvp.claim.service.letter;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.claim.service.letter.strategy.LetterStrategy;
import com.claims.mvp.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ClaimLetterService.
 * <p>
 * Generates the customer-facing claim letter for an airline.
 * Validates claim preconditions, then delegates body generation to
 * the {@link LetterStrategy} matching the issue type.
 * <p>
 * To support a new claim type, add a new strategy bean — no changes here.
 */

@Service
public class ClaimLetterServiceImpl implements ClaimLetterService {
    private final Map<IssueType, LetterStrategy> strategiesByType;

    public ClaimLetterServiceImpl(List<LetterStrategy> strategies) {
        this.strategiesByType = strategies.stream()
                .collect(Collectors.toMap(
                        LetterStrategy::supportedIssueType,
                        Function.identity()
                ));
    }

    @Override
    public LetterResponse generateLetter(Claim claim) {
        validate(claim);

        IssueType issueType = claim.getIssue().getType();
        LetterStrategy strategy = strategiesByType.get(issueType);
        if (strategy == null) {
            throw new IllegalArgumentException("No letter strategy for type : " + issueType);
        }
        return strategy.generateLetter(claim);
    }

    private void validate(Claim claim) {
       if(claim == null){
           throw new IllegalArgumentException("Claim must not be null");
       }

       User user = claim.getUser();
       if(user == null){
           throw new IllegalArgumentException("User must not be null");
       }
       Flight flight = claim.getFlight();
       if(flight == null){
           throw new IllegalArgumentException("Flight must not be null");
       }
       Issue issue = claim.getIssue();
       if(issue == null){
           throw new IllegalArgumentException("Issue must not be null");
       }
    }


}
