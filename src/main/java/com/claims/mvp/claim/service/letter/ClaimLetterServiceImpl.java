package com.claims.mvp.claim.service.letter;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.user.model.User;
import org.springframework.stereotype.Service;

@Service
public class ClaimLetterServiceImpl implements ClaimLetterService {

    @Override
    public LetterResponse generateLetter(Claim claim) {
        if (claim == null) {
            throw new IllegalArgumentException("Claim cannot be null");
        }
        User user = claim.getUser();
        if (user == null || user.getFullName() == null || user.getFullName().isBlank()) {
            throw new IllegalArgumentException("User must have a full name");
        }
        Flight flight = claim.getFlight();
        Issue issue = claim.getIssue();
        if (flight == null || issue == null) {
            throw new IllegalArgumentException("Flight and issue must be set");
        }

        String subject = "EU261 Compensation Claim - " + flight.getFlightNumber()
                + " (" + flight.getRouteFrom() + " -> " + flight.getRouteTo() + ")";


        String incidentLine = switch (issue.getType()) {
            case DELAY -> {
                Integer delayMinutes = issue.getDelayMinutes();
                yield "My flight was delayed by " + (delayMinutes == null ? "unknown" : delayMinutes) + " minutes.";
            }
            case CANCELLATION -> {
                Integer days = issue.getCancellationNoticeDays();
                yield "My flight was cancelled. I was notified "
                        + (days == null ? "N/A" : days) + " days before departure.";
            }
            default -> throw new IllegalArgumentException("Unsupported issue type: " + issue.getType());

        };
        String body = """
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
                """.formatted(
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