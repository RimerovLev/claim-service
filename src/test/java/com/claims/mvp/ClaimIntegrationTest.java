package com.claims.mvp;

import com.claims.mvp.dto.*;
import com.claims.mvp.dto.enums.IssueType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClaimIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Test
    void createClaim_thenGetById_andEvaluate() {
        UserDto user = createUser("Ivan Petrov", "ivan@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of());

        RestClient client = RestClient.create("http://localhost:" + port);
        ResponseEntity<ClaimResponse> createResponse =
                client.post()
                        .uri("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(createRequest)
                        .retrieve()
                        .toEntity(ClaimResponse.class);
        assertThat(createResponse.getStatusCode().is2xxSuccessful()).isTrue();
        ClaimResponse created = createResponse.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getEligible()).isTrue();
        assertThat(created.getCompensationAmount()).isEqualTo(400);

        ClaimResponse fetched = client.get()
                .uri("/api/claims/" + created.getId())
                .retrieve()
                .body(ClaimResponse.class);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(created.getId());

        EvaluateClaimRequest evaluateRequest = new EvaluateClaimRequest();
        evaluateRequest.setIssue(buildCancellationIssue(10));
        evaluateRequest.setEuContext(buildEuContext(true, true));
        evaluateRequest.setFlight(buildFlight(3500));

        ResponseEntity<ClaimResponse> evaluateResponse =
                client.post()
                        .uri("/api/claims/" + created.getId() + "/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(evaluateRequest)
                        .retrieve()
                        .toEntity(ClaimResponse.class);
        assertThat(evaluateResponse.getStatusCode().is2xxSuccessful()).isTrue();
        ClaimResponse evaluated = evaluateResponse.getBody();
        assertThat(evaluated).isNotNull();
        assertThat(evaluated.getEligible()).isTrue();
        assertThat(evaluated.getCompensationAmount()).isEqualTo(400);
    }

    @Test
    void createClaim_userNotFound_returns404() {
        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(999999L);
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of());

        RestClient client = RestClient.create("http://localhost:" + port);
        int status = client.post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(404);
    }

    @Test
    void getClaim_notFound_returns404() {
        RestClient client = RestClient.create("http://localhost:" + port);
        int status = client.get()
                .uri("/api/claims/999999")
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(404);
    }

    @Test
    void createUser_duplicateEmail_returns409() {
        createUser("Lev Rimerov", "dupe@example.com");

        UserDto request = new UserDto();
        request.setFullName("Lev Rimerov");
        request.setEmail("dupe@example.com");

        RestClient client = RestClient.create("http://localhost:" + port);
        int status = client.post()
                .uri("/api/claims/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(409);
    }

    private UserDto createUser(String fullName, String email) {
        UserDto request = new UserDto();
        request.setFullName(fullName);
        request.setEmail(email);

        RestClient client = RestClient.create("http://localhost:" + port);
        ResponseEntity<UserDto> response =
                client.post()
                        .uri("/api/claims/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .toEntity(UserDto.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto created = response.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        return created;
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

    private IssueDto buildCancellationIssue(int noticeDays) {
        IssueDto issue = new IssueDto();
        issue.setType(IssueType.CANCELLATION);
        issue.setDelayMinutes(null);
        issue.setCancellationNoticeDays(noticeDays);
        issue.setExtraordinaryCircumstances(false);
        return issue;
    }

    private EuContextDto buildEuContext(boolean departureFromEu, boolean euCarrier) {
        EuContextDto ctx = new EuContextDto();
        ctx.setDepartureFromEu(departureFromEu);
        ctx.setEuCarrier(euCarrier);
        return ctx;
    }
}
