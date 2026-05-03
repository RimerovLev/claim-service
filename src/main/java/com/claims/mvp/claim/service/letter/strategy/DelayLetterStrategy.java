package com.claims.mvp.claim.service.letter.strategy;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import org.springframework.stereotype.Component;

/**
 * Letter for flight delay claims under EU 261/2004.
 */
@Component
public class DelayLetterStrategy implements LetterStrategy{

    private static final String BODY_TEMPLATE = """
    Dear %s Customer Relations Team,
            
            I am submitting a compensation claim under EU Regulation 261/2004.
            
            Passenger: %s
            Booking reference: %s
            
            Flight number: %s
            Flight date: %s
            Route: %s → %s
            
            Incident: %s
            
            Based on the information provided, the calculated compensation amount is: %s EUR.
            
            Please confirm receipt of this claim and provide a reference number. If additional documents are required, please specify which ones.
            
            Sincerely,
            %s
            """;


    @Override
    public IssueType supportedIssueType() {
        return IssueType.DELAY;
    }

    @Override
    public LetterResponse generateLetter(Claim claim) {
        Flight flight = claim.getFlight();
        Issue issue = claim.getIssue();
        String subject = "EU 261 Compensation Claim - "
                + flight.getFlightNumber()
                + " (" + flight.getRouteFrom() + " -> "
                + flight.getRouteTo() + ")";

        Integer delayMinutes = issue.getDelayMinutes();

        String incidentLine = "My filght was delayed by "
                + (delayMinutes == null ? "unknown" : delayMinutes) + " minutes.";

        String body = BODY_TEMPLATE.formatted(
                flight.getAirline(),
                claim.getUser().getFullName(),
                flight.getBookingRef(),
                flight.getFlightNumber(),
                flight.getFlightDate(),
                flight.getRouteFrom(),
                flight.getRouteTo(),
                incidentLine,
                claim.getCompensationAmount() == null ? "0" : claim.getCompensationAmount().toString(),
                claim.getUser().getFullName()
        );
        return new LetterResponse(subject, body);
    }
}
