package com.claims.mvp;

import com.claims.mvp.claim.dto.BoardingDocumentDto;
import com.claims.mvp.claim.dto.ClaimResponse;
import com.claims.mvp.claim.dto.CreateClaimRequest;
import com.claims.mvp.claim.dto.EuContextDto;
import com.claims.mvp.claim.dto.FlightDto;
import com.claims.mvp.claim.dto.IssueDto;
import com.claims.mvp.claim.dto.StatusChangeRequest;
import com.claims.mvp.claim.dto.UpdateClaimDetails;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.events.dto.EventsResponseDto;
import com.claims.mvp.user.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.context.SpringBootTest(webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClaimIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Test
    void createClaim_withoutDocuments_setsDocsRequested() {
        UserDto user = createUser("Ivan Petrov", "ivan-create@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of());

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.DOCS_REQUESTED);
        assertThat(created.getEligible()).isTrue();
        assertThat(created.getCompensationAmount()).isEqualTo(400);
    }

    @Test
    void createClaim_withRequiredDocuments_setsReadyToSubmit() {
        UserDto user = createUser("Lev Rimerov", "lev-ready@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of(
                buildDocument("ticket-1", DocumentTypes.TICKET),
                buildDocument("boarding-pass-1", DocumentTypes.BOARDING_PASS)
        ));

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);
        assertThat(created.getDocuments()).hasSize(2);
    }

    @Test
    void updateClaimDetails_withMissingDocuments_promotesToReadyToSubmit() {
        UserDto user = createUser("Upload Later", "later@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of());

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        UpdateClaimDetails updateRequest = new UpdateClaimDetails();
        updateRequest.setDocuments(List.of(
                buildDocument("ticket-2", DocumentTypes.TICKET),
                buildDocument("boarding-pass-2", DocumentTypes.BOARDING_PASS)
        ));

        ClaimResponse updated = client().patch()
                .uri("/api/claims/" + created.getId() + "/update")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);
        assertThat(updated.getDocuments()).hasSize(2);
    }

    @Test
    void updateClaimStatus_validTransition_createsEvent() {
        UserDto user = createUser("Workflow User", "workflow@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of(
                buildDocument("ticket-3", DocumentTypes.TICKET),
                buildDocument("boarding-pass-3", DocumentTypes.BOARDING_PASS)
        ));

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        ClaimResponse updatedStatus = client().post()
                .uri("/api/claims/" + created.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StatusChangeRequest(ClaimStatus.SUBMITTED, "sent via email"))
                .retrieve()
                .body(ClaimResponse.class);

        List<EventsResponseDto> events = client().get()
                .uri("/api/claims/" + created.getId() + "/events")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(updatedStatus).isNotNull();
        assertThat(updatedStatus.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(events).isNotEmpty();
        assertThat(events.getFirst().getPayload()).contains("\"from\":\"READY_TO_SUBMIT\"");
        assertThat(events.getFirst().getPayload()).contains("\"to\":\"SUBMITTED\"");
    }

    @Test
    void updateClaimStatus_invalidTransition_returns409() {
        UserDto user = createUser("Invalid Workflow", "invalid-workflow@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of());

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        int status = client().post()
                .uri("/api/claims/" + created.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StatusChangeRequest(ClaimStatus.PAID, "skip workflow"))
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(409);
    }

    @Test
    void getClaimEvents_notFound_returns404() {
        int status = client().get()
                .uri("/api/claims/999999/events")
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(404);
    }

    @Test
    void createUser_duplicateEmail_returns409() {
        createUser("Dup User", "dupe@example.com");

        UserDto request = new UserDto();
        request.setFullName("Dup User");
        request.setEmail("dupe@example.com");

        int status = client().post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(409);
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private UserDto createUser(String fullName, String email) {
        UserDto request = new UserDto();
        request.setFullName(fullName);
        request.setEmail(email);

        ResponseEntity<UserDto> response = client().post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(UserDto.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        return response.getBody();
    }

    private FlightDto buildFlight(int distanceKm) {
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

    private IssueDto buildDelayIssue(int delayMinutes) {
        IssueDto issue = new IssueDto();
        issue.setType(IssueType.DELAY);
        issue.setDelayMinutes(delayMinutes);
        issue.setCancellationNoticeDays(null);
        issue.setExtraordinaryCircumstances(false);
        return issue;
    }

    private EuContextDto buildEuContext(boolean departureFromEu, boolean euCarrier) {
        EuContextDto ctx = new EuContextDto();
        ctx.setDepartureFromEu(departureFromEu);
        ctx.setEuCarrier(euCarrier);
        return ctx;
    }

    private BoardingDocumentDto buildDocument(String id, DocumentTypes type) {
        BoardingDocumentDto document = new BoardingDocumentDto();
        document.setId(id);
        document.setType(type);
        document.setUrl("https://example.test/" + id);
        return document;
    }
}
