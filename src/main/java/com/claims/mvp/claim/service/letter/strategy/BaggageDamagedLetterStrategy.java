package com.claims.mvp.claim.service.letter.strategy;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import org.springframework.stereotype.Component;

@Component
public class BaggageDamagedLetterStrategy implements LetterStrategy{
    private static final String BODY_TEMPLATE = """
        Dear %s Baggage Services Team,
        
        I am submitting a baggage damage compensation claim under
        the Montreal Convention 1999 (Article 17).
        
        Passenger: %s
        Booking reference: %s
        
        Flight number: %s
        Flight date: %s
        Route: %s → %s
        
        Upon arrival, I discovered that my checked baggage was damaged.
        A Property Irregularity Report (PIR) was filed at the airport.
        
        This claim is being submitted within the 7-day period required under the Montreal Convention.
        
        I am requesting compensation in the amount of %s EUR for the repair
        or replacement of the damaged baggage and its contents.
        
        Supporting documents (photos, receipts, and repair estimates) can be provided upon request.
        
        Please confirm receipt of this claim and provide a reference number.
        
        Sincerely,
        %s
        """;

    @Override
    public IssueType supportedIssueType() {
        return IssueType.BAGGAGE_DAMAGED;
    }

    @Override
    public LetterResponse generateLetter(Claim claim) {
        Flight flight = claim.getFlight();
        String subject = "Baggage Damage Compensation Claim - "
                + flight.getFlightNumber()
                + " (" + flight.getRouteFrom() + " -> " + flight.getRouteTo() + ")";
        String body = BODY_TEMPLATE.formatted(
                flight.getAirline(),                  // %s 1 — Dear X Baggage Services Team
                claim.getUser().getFullName(),         // %s 2 — Passenger
                flight.getBookingRef(),                // %s 3 — Booking reference
                flight.getFlightNumber(),              // %s 4 — Flight number
                flight.getFlightDate(),                // %s 5 — Flight date
                flight.getRouteFrom(),                 // %s 6 — Route from
                flight.getRouteTo(),                   // %s 7 — Route to
                claim.getCompensationAmount() ==       // %s 8 — EUR amount
                        null ? "0" : claim.getCompensationAmount().toString(),
                claim.getUser().getFullName()          // %s 9 — Sincerely
        );
        return new LetterResponse(subject, body);
    }
}
