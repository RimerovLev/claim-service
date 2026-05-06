package com.claims.mvp;

import com.claims.mvp.claim.dto.request.*;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.events.dto.response.EventsResponse;
import com.claims.mvp.user.dto.request.CreateUserRequest;
import com.claims.mvp.user.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.context.SpringBootTest(webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClaimIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Test
    void createClaim_withoutDocuments_setsDocsRequested() {
        UserResponse user = createUser("Ivan Petrov", "ivan-create@example.com");

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
        UserResponse user = createUser("Lev Rimerov", "lev-ready@example.com");

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
        UserResponse user = createUser("Upload Later", "later@example.com");

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

        UpdateClaimDetailsRequest updateRequest = new UpdateClaimDetailsRequest();
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
        UserResponse user = createUser("Workflow User", "workflow@example.com");

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
                .uri("/api/claims/" + created.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StatusChangeRequest(ClaimStatus.SUBMITTED, "sent via email"))
                .retrieve()
                .body(ClaimResponse.class);

        List<EventsResponse> events = client().get()
                .uri("/api/claims/" + created.getId() + "/events")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(updatedStatus).isNotNull();
        assertThat(updatedStatus.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(events).isNotEmpty();
        assertThat(events.getFirst().getPayload()).contains("\"from\":\"READY_TO_SUBMIT\"");
        assertThat(events.getFirst().getPayload()).contains("\"to\":\"SUBMITTED\"");
    }

    @Test
    void updateClaimStatus_invalidTransition_returns409() {
        UserResponse user = createUser("Invalid Workflow", "invalid-workflow@example.com");

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
                .uri("/api/claims/" + created.getId() + "/transition")
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
    void getClaimById_existingClaim_returnsClaim() {
        UserResponse user = createUser("Get Claim User", "get-claim@example.com");

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

        ClaimResponse fetched = client().get()
                .uri("/api/claims/" + created.getId())
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getStatus()).isEqualTo(ClaimStatus.DOCS_REQUESTED);
    }

    @Test
    void getAllClaims_returnsCreatedClaims() {
        UserResponse user = createUser("List User", "list-user@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of());

        client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        PageDto<ClaimResponse> pageDto = client().get()
                .uri("/api/claims")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(pageDto).isNotNull();
        assertThat(pageDto.content).isNotEmpty();
    }

    @Test
    void createUser_duplicateEmail_returns409() {
        createUser("Dup User", "dupe@example.com");

        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Dup User");
        request.setEmail("dupe@example.com");

        int status = client().post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(409);
    }

    @Test
    void createClaim_triggersNotification() {
        UserResponse user = createUser("Notify User", "notify@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of());

        client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        org.mockito.Mockito.verify(mailSender)
                .send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void createUser_invalidEmail_returns400AndValidationMessage() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Bad Email");
        request.setEmail("not-an-email");

        ResponseEntity<ErrorResponse> response = client().post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((req, res) -> ResponseEntity.status(res.getStatusCode())
                        .body(res.bodyTo(ErrorResponse.class)));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("почты");
    }

    @Test
    void createClaim_missingRequiredFields_returns400() {
        CreateClaimRequest request = new CreateClaimRequest();

        int status = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(400);
    }

    @Test
    void createClaim_baggageDelayed_withRequiredDocuments_isEligibleReadyToSubmit_andLetterMentionsMontreal() {
        UserResponse user = createUser("Baggage User", "baggage-user@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildBaggageDelayedIssue(24));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of(
                buildDocument("pir-1", DocumentTypes.PIR),
                buildDocument("bag-tag-1", DocumentTypes.BAG_TAG)
        ));

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.getEligible()).isTrue();
        assertThat(created.getCompensationAmount()).isEqualTo(50);
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);

        LetterResponse letter = client().get()
                .uri("/api/claims/" + created.getId() + "/letter")
                .retrieve()
                .body(LetterResponse.class);

        assertThat(letter).isNotNull();
        assertThat(letter.getBody()).contains("Montreal Convention");
        assertThat(letter.getBody()).contains("Article 19");
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private UserResponse createUser(String fullName, String email) {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName(fullName);
        request.setEmail(email);

        ResponseEntity<UserResponse> response = client().post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(UserResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        return response.getBody();
    }

    private FlightRequest buildFlight(int distanceKm) {
        FlightRequest flight = new FlightRequest();
        flight.setFlightNumber("LH123");
        flight.setFlightDate(LocalDate.of(2026, 3, 1));
        flight.setRouteFrom("FRA");
        flight.setRouteTo("MAD");
        flight.setAirline("Lufthansa");
        flight.setBookingRef("ABC123");
        flight.setDistanceKm(distanceKm);
        return flight;
    }

    private IssueRequest buildDelayIssue(int delayMinutes) {
        IssueRequest issue = new IssueRequest();
        issue.setType(IssueType.DELAY);
        issue.setDelayMinutes(delayMinutes);
        issue.setCancellationNoticeDays(null);
        issue.setExtraordinaryCircumstances(false);
        return issue;
    }

    private IssueRequest buildBaggageDelayedIssue(int baggageDelayHours) {
        IssueRequest issue = new IssueRequest();
        issue.setType(IssueType.BAGGAGE_DELAYED);
        issue.setBaggageDelayHours(baggageDelayHours);
        issue.setExtraordinaryCircumstances(false);
        return issue;
    }

    private IssueRequest buildBaggageLostIssue(int baggageDelayHours) {
        IssueRequest issue = new IssueRequest();
        issue.setType(IssueType.BAGGAGE_LOST);
        issue.setBaggageDelayHours(baggageDelayHours);
        issue.setExtraordinaryCircumstances(false);
        return issue;
    }

    @Test
    void createClaim_baggageLost_withRequiredDocuments_isEligibleReadyToSubmit_andLetterMentionsArticle17() {
        UserResponse user = createUser("Baggage Lost User", "baggage-lost-user@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildBaggageLostIssue(600));  // 600 часов > 504 часов (21 дня) → eligible
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of(
                buildDocument("pir-lost", DocumentTypes.PIR),
                buildDocument("bag-tag-lost", DocumentTypes.BAG_TAG)
        ));

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.getEligible()).isTrue();
        assertThat(created.getCompensationAmount()).isEqualTo(1000);
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);

        LetterResponse letter = client().get()
                .uri("/api/claims/" + created.getId() + "/letter")
                .retrieve()
                .body(LetterResponse.class);

        assertThat(letter).isNotNull();
        assertThat(letter.getBody()).contains("Article 17");
        assertThat(letter.getBody()).contains("Montreal Convention");
    }

    private IssueRequest buildMissedConnectionIssue(int totalArrivalDelayMinutes) {
        IssueRequest issue = new IssueRequest();
        issue.setType(IssueType.MISSED_CONNECTION);
        issue.setDelayMinutes(totalArrivalDelayMinutes);
        issue.setCancellationNoticeDays(null);
        issue.setExtraordinaryCircumstances(false);
        return issue;
    }

    private EuContextRequest buildEuContext(boolean departureFromEu, boolean euCarrier) {
        EuContextRequest ctx = new EuContextRequest();
        ctx.setDepartureFromEu(departureFromEu);
        ctx.setEuCarrier(euCarrier);
        return ctx;
    }

    private BoardingDocumentRequest buildDocument(String id, DocumentTypes type) {
        BoardingDocumentRequest document = new BoardingDocumentRequest();
        document.setId(id);
        document.setType(type);
        document.setUrl("https://example.test/" + id);
        return document;
    }

    @Test
    void getClaimLetter_existingClaim_returnsSubjectAndBody() {
        UserResponse user = createUser("Letter User", "letter-user@example.com");

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
        assertThat(created.getId()).isNotNull();

        LetterResponse letter = client().get()
                .uri("/api/claims/" + created.getId() + "/letter")
                .retrieve()
                .body(LetterResponse.class);

        assertThat(letter).isNotNull();
        assertThat(letter.getSubject()).isNotBlank();
        assertThat(letter.getBody()).isNotBlank();
        assertThat(letter.getSubject()).contains("LH123");
        assertThat(letter.getBody()).contains("Letter User");
        assertThat(letter.getBody()).contains("ABC123");
        assertThat(letter.getBody()).contains("FRA");
        assertThat(letter.getBody()).contains("MAD");
        assertThat(letter.getBody()).contains("220");
    }

    @Test
    void createClaim_missedConnection_isEligibleAndGeneratesLetter() {
        UserResponse user = createUser("Missed Connection User", "missed-connection@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildMissedConnectionIssue(240));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of(
                buildDocument("ticket-mc", DocumentTypes.TICKET),
                buildDocument("boarding-pass-mc", DocumentTypes.BOARDING_PASS)
        ));

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.getEligible()).isTrue();
        assertThat(created.getCompensationAmount()).isEqualTo(400);
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);

        LetterResponse letter = client().get()
                .uri("/api/claims/" + created.getId() + "/letter")
                .retrieve()
                .body(LetterResponse.class);

        assertThat(letter).isNotNull();
        assertThat(letter.getSubject()).contains("EU 261");
        assertThat(letter.getSubject()).contains("LH123");
        assertThat(letter.getBody()).contains("Missed Connection User");
        assertThat(letter.getBody()).contains("240 minutes");
    }

    @Test
    void submitClaim_readyToSubmit_setsSubmittedStatus() {
        UserResponse user = createUser("Submit User", "submit-user@example.com");

        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of(
                buildDocument("submit-ticket-1", DocumentTypes.TICKET),
                buildDocument("submit-boarding-1", DocumentTypes.BOARDING_PASS)
        ));

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);

        StatusChangeRequest submitRequest = new StatusChangeRequest(ClaimStatus.SUBMITTED, "submitted via email");

        ClaimResponse submitted = client().post()
                .uri("/api/claims/" + created.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(submitRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(submitted).isNotNull();
        assertThat(submitted.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    @Test
    void submitClaim_docsRequested_returns409() {
        UserResponse user = createUser("Submit Blocked", "submit-blocked@example.com");

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

        StatusChangeRequest submitRequest = new StatusChangeRequest(ClaimStatus.SUBMITTED, "trying to submit too early");

        int status = client().post()
                .uri("/api/claims/" + created.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(submitRequest)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(409);
    }

    @Test
    void sendFollowUp_afterSubmit_setsFollowUpSentStatus() {
        UserResponse user = createUser("Follow Up User", "follow-up-user@example.com");

        ClaimResponse created = createReadyToSubmitClaim(user, "follow-up");
        ClaimResponse submitted = submitClaim(created.getId(), "submitted via email");

        StatusChangeRequest followUpRequest = new StatusChangeRequest(ClaimStatus.FOLLOW_UP_SENT, "second reminder sent");

        ClaimResponse followedUp = client().post()
                .uri("/api/claims/" + submitted.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(followUpRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(followedUp).isNotNull();
        assertThat(followedUp.getStatus()).isEqualTo(ClaimStatus.FOLLOW_UP_SENT);
    }

    @Test
    void approveClaim_afterFollowUp_setsApprovedStatus() {
        UserResponse user = createUser("Approve User", "approve-user@example.com");

        ClaimResponse created = createReadyToSubmitClaim(user, "approve");
        ClaimResponse submitted = submitClaim(created.getId(), "submitted via email");
        ClaimResponse followedUp = sendFollowUp(submitted.getId(), "follow-up sent");

        StatusChangeRequest approveRequest = new StatusChangeRequest(ClaimStatus.APPROVED, "approved via email");

        ClaimResponse approved = client().post()
                .uri("/api/claims/" + followedUp.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(approveRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(approved).isNotNull();
        assertThat(approved.getStatus()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void markClaimAsPaid_afterApproval_setsPaidStatus() {
        UserResponse user = createUser("Paid User", "paid-user@example.com");
        ClaimResponse created = createReadyToSubmitClaim(user, "paid");
        ClaimResponse submitted = submitClaim(created.getId(), "submitted via email");
        ClaimResponse followedUp = sendFollowUp(submitted.getId(), "follow-up sent");
        ClaimResponse approved = approveClaim(followedUp.getId(), "approved by airline");

        StatusChangeRequest paidRequest = new StatusChangeRequest(ClaimStatus.PAID, "compensation received");

        ClaimResponse paid = client().post()
                .uri("/api/claims/" + approved.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(paidRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(paid).isNotNull();
        assertThat(paid.getStatus()).isEqualTo(ClaimStatus.PAID);
    }

    @Test
    void approveClaim_fromReadyToSubmit_returns409() {
        UserResponse user = createUser("Approve Too Early", "approve-too-early@example.com");
        ClaimResponse created = createReadyToSubmitClaim(user, "early-approve");
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);

        StatusChangeRequest approveRequest = new StatusChangeRequest(ClaimStatus.APPROVED, "trying to approve too early");

        int status = client().post()
                .uri("/api/claims/" + created.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(approveRequest)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(409);
    }

    @Test
    void rejectClaim_afterFollowUp_setsRejectedStatus() {
        UserResponse user = createUser("Reject User", "reject-user@example.com");
        ClaimResponse created = createReadyToSubmitClaim(user, "reject");
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);
        ClaimResponse submitted = submitClaim(created.getId(), "submitted via email");
        ClaimResponse followedUp = sendFollowUp(submitted.getId(), "follow-up sent after no response");

        StatusChangeRequest rejectRequest = new StatusChangeRequest(ClaimStatus.REJECTED, "airline rejected the claim");

        ClaimResponse rejected = client().post()
                .uri("/api/claims/" + created.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(rejectRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(rejected).isNotNull();
        assertThat(rejected.getStatus()).isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void closeClaim_afterPaid_setsClosedStatus() {
        UserResponse user = createUser("Close Paid User", "close-paid@example.com");

        ClaimResponse created = createReadyToSubmitClaim(user, "close-paid");
        ClaimResponse submitted = submitClaim(created.getId(), "submitted via email");
        ClaimResponse followedUp = sendFollowUp(submitted.getId(), "follow-up sent");
        ClaimResponse approved = approveClaim(followedUp.getId(), "approved");
        ClaimResponse paid = markClaimAsPaid(approved.getId(), "paid");

        StatusChangeRequest closeRequest = new StatusChangeRequest(ClaimStatus.CLOSED, "claim completed");

        ClaimResponse closed = client().post()
                .uri("/api/claims/" + paid.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(closeRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(closed).isNotNull();
        assertThat(closed.getStatus()).isEqualTo(ClaimStatus.CLOSED);
    }

    @Test
    void closeClaim_afterRejected_setsClosedStatus() {
        UserResponse user = createUser("Close Rejected User", "close-rejected@example.com");

        ClaimResponse created = createReadyToSubmitClaim(user, "close-rejected");
        ClaimResponse submitted = submitClaim(created.getId(), "submitted via email");
        ClaimResponse followedUp = sendFollowUp(submitted.getId(), "follow-up sent");
        ClaimResponse rejected = rejectClaim(followedUp.getId(), "rejected");

        StatusChangeRequest closeRequest = new StatusChangeRequest(ClaimStatus.CLOSED, "claim archived");

        ClaimResponse closed = client().post()
                .uri("/api/claims/" + rejected.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(closeRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(closed).isNotNull();
        assertThat(closed.getStatus()).isEqualTo(ClaimStatus.CLOSED);
    }

    @Test
    void closeClaim_fromApproved_returns409() {
        UserResponse user = createUser("Close Too Early", "close-too-early@example.com");

        ClaimResponse created = createReadyToSubmitClaim(user, "close-early");
        ClaimResponse submitted = submitClaim(created.getId(), "submitted via email");
        ClaimResponse followedUp = sendFollowUp(submitted.getId(), "follow-up sent");
        ClaimResponse approved = approveClaim(followedUp.getId(), "approved");

        StatusChangeRequest closeRequest = new StatusChangeRequest(ClaimStatus.CLOSED, "trying to close too early");

        int status = client().post()
                .uri("/api/claims/" + approved.getId() + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(closeRequest)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(409);
    }

    private ClaimResponse createReadyToSubmitClaim(UserResponse user, String suffix) {
        CreateClaimRequest createRequest = new CreateClaimRequest();
        createRequest.setUserId(user.getId());
        createRequest.setFlight(buildFlight(1800));
        createRequest.setIssue(buildDelayIssue(220));
        createRequest.setEuContext(buildEuContext(true, true));
        createRequest.setDocuments(List.of(
                buildDocument(suffix + "-ticket", DocumentTypes.TICKET),
                buildDocument(suffix + "-boarding", DocumentTypes.BOARDING_PASS)
        ));

        ClaimResponse created = client().post()
                .uri("/api/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);
        return created;
    }

    private ClaimResponse transitionClaim(Long id, ClaimStatus target, String note) {
        StatusChangeRequest request = new StatusChangeRequest(target, note);

        ClaimResponse response = client().post()
                .uri("/api/claims/" + id + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ClaimResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(target);
        return response;
    }

    private ClaimResponse submitClaim(Long id, String note) {
        return transitionClaim(id, ClaimStatus.SUBMITTED, note);
    }

    private ClaimResponse sendFollowUp(Long id, String note) {
        return transitionClaim(id, ClaimStatus.FOLLOW_UP_SENT, note);
    }

    private ClaimResponse approveClaim(Long id, String note) {
        return transitionClaim(id, ClaimStatus.APPROVED, note);
    }

    private ClaimResponse rejectClaim(Long id, String note) {
        return transitionClaim(id, ClaimStatus.REJECTED, note);
    }

    private ClaimResponse markClaimAsPaid(Long id, String note) {
        return transitionClaim(id, ClaimStatus.PAID, note);
    }

    private record ErrorResponse(String message) {
    }

    private record PageDto<T>(
            List<T> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ){}


}
