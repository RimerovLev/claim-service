package com.claims.mvp.claim.service.letter.strategy;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import org.springframework.stereotype.Component;

@Component
public class BaggageDelayedLetterStrategy implements LetterStrategy {
    private static final String BODY_TEMPLATE = """
            Dear %s Baggage Services Team,
            
            I am submitting a baggage delay compensation claim under
            the Montreal Convention 1999 (Article 19).
            
            Passenger: %s
            Booking reference: %s
            
            Flight number: %s
            Flight date: %s
            Route: %s → %s
            
            My checked baggage was delayed by %s hours upon arrival.
            A Property Irregularity Report (PIR) has been filed at the airport.
            
            I am requesting reimbursement in the amount of %s EUR for
            reasonable expenses incurred during the delay.
            
            Please confirm receipt of this claim and provide a reference number.
            
            Sincerely,
            %s
            """;

    @Override
    public IssueType supportedIssueType() {
        return IssueType.BAGGAGE_DELAYED;
    }

    @Override
    public LetterResponse generateLetter(Claim claim) {
        Flight flight = claim.getFlight();
        Issue issue = claim.getIssue();
        String subject = "Baggage Delay Compensation Claim - "
                + flight.getFlightNumber()
                + " (" + flight.getRouteFrom() + " -> "
                + flight.getRouteTo() + ")";

        Integer delayHours = issue.getBaggageDelayHours();

        String delayHoursText = delayHours == null ? "unknown" : delayHours.toString();

        String body = BODY_TEMPLATE.formatted(
                flight.getAirline(),
                claim.getUser().getFullName(),
                flight.getBookingRef(),
                flight.getFlightNumber(),
                flight.getFlightDate(),
                flight.getRouteFrom(),
                flight.getRouteTo(),
                delayHoursText,
                claim.getCompensationAmount() == null ? "0" : claim.getCompensationAmount().toString(),
                claim.getUser().getFullName()
        );
        return new LetterResponse(subject, body);
    }
}
