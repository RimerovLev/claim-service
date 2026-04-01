package com.claims.mvp.eligibility;

import com.claims.mvp.claim.dto.BoardingDocumentDto;
import com.claims.mvp.claim.dto.EuContextDto;
import com.claims.mvp.claim.dto.FlightDto;
import com.claims.mvp.claim.dto.IssueDto;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.eligibility.dto.EligibilityResult;
import com.claims.mvp.eligibility.service.EligibilityServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityServiceImplTest {

    private final EligibilityServiceImpl service = new EligibilityServiceImpl();

    @Test
    void delayEligible_inScope_noExtraordinary() {
        EligibilityResult result = service.evaluate(
                issue(IssueType.DELAY, 180, null, false),
                flight(1800),
                euContext(true, false),
                List.of()
        );

        assertThat(result.getEligible()).isTrue();
        assertThat(result.getCompensationAmount()).isEqualTo(400);
        assertThat(result.getRequiredDocuments())
                .containsExactly(DocumentTypes.TICKET, DocumentTypes.BOARDING_PASS);
    }

    @Test
    void delayNotEligible_belowThreshold_keepsFlightDocuments() {
        EligibilityResult result = service.evaluate(
                issue(IssueType.DELAY, 100, null, false),
                flight(1800),
                euContext(true, true),
                List.of()
        );

        assertThat(result.getEligible()).isFalse();
        assertThat(result.getCompensationAmount()).isEqualTo(0);
        assertThat(result.getRequiredDocuments())
                .containsExactly(DocumentTypes.TICKET, DocumentTypes.BOARDING_PASS);
    }

    @Test
    void cancellationEligible_noticeWithin14Days() {
        EligibilityResult result = service.evaluate(
                issue(IssueType.CANCELLATION, null, 10, false),
                flight(1200),
                euContext(true, true),
                List.of(document(DocumentTypes.TICKET), document(DocumentTypes.BOARDING_PASS))
        );

        assertThat(result.getEligible()).isTrue();
        assertThat(result.getCompensationAmount()).isEqualTo(250);
    }

    @Test
    void extraordinaryCircumstances_notEligible() {
        EligibilityResult result = service.evaluate(
                issue(IssueType.DELAY, 200, null, true),
                flight(1200),
                euContext(true, true),
                List.of()
        );

        assertThat(result.getEligible()).isFalse();
        assertThat(result.getCompensationAmount()).isEqualTo(0);
    }

    @Test
    void outOfScope_notEligible() {
        EligibilityResult result = service.evaluate(
                issue(IssueType.DELAY, 200, null, false),
                flight(1200),
                euContext(false, false),
                List.of()
        );

        assertThat(result.getEligible()).isFalse();
        assertThat(result.getCompensationAmount()).isEqualTo(0);
    }

    @Test
    void compensationTiers() {
        assertThat(service.calculateCompensationAmount(1500)).isEqualTo(250);
        assertThat(service.calculateCompensationAmount(3500)).isEqualTo(400);
        assertThat(service.calculateCompensationAmount(4000)).isEqualTo(600);
    }

    private IssueDto issue(IssueType type, Integer delayMinutes, Integer cancellationNoticeDays, boolean extraordinary) {
        IssueDto issue = new IssueDto();
        issue.setType(type);
        issue.setDelayMinutes(delayMinutes);
        issue.setCancellationNoticeDays(cancellationNoticeDays);
        issue.setExtraordinaryCircumstances(extraordinary);
        return issue;
    }

    private FlightDto flight(int distanceKm) {
        FlightDto flight = new FlightDto();
        flight.setFlightNumber("LH123");
        flight.setFlightDate(LocalDate.of(2026, 3, 1));
        flight.setRouteFrom("FRA");
        flight.setRouteTo("MAD");
        flight.setAirline("Lufthansa");
        flight.setBookingRef("ABC123");
        flight.setDistanceKm(distanceKm);
        return flight;
    }

    private EuContextDto euContext(boolean departureFromEu, boolean euCarrier) {
        EuContextDto ctx = new EuContextDto();
        ctx.setDepartureFromEu(departureFromEu);
        ctx.setEuCarrier(euCarrier);
        return ctx;
    }

    private BoardingDocumentDto document(DocumentTypes type) {
        BoardingDocumentDto document = new BoardingDocumentDto();
        document.setId(type.name().toLowerCase());
        document.setType(type);
        document.setUrl("https://example.test/" + type.name().toLowerCase());
        return document;
    }
}
