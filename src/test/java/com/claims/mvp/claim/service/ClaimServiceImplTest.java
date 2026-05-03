package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dao.ClaimRepository;
import com.claims.mvp.claim.dto.request.BoardingDocumentRequest;
import com.claims.mvp.claim.dto.request.CreateClaimRequest;
import com.claims.mvp.claim.dto.request.EuContextRequest;
import com.claims.mvp.claim.dto.request.FlightRequest;
import com.claims.mvp.claim.dto.request.IssueRequest;
import com.claims.mvp.claim.dto.request.StatusChangeRequest;
import com.claims.mvp.claim.dto.request.UpdateClaimDetailsRequest;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.EventTypes;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.claim.mapper.ClaimEntityMapper;
import com.claims.mvp.claim.mapper.ClaimMapper;
import com.claims.mvp.eligibility.service.EligibilityService;
import com.claims.mvp.eligibility.service.EligibilityServiceImpl;
import com.claims.mvp.claim.service.documents.ClaimDocumentsService;
import com.claims.mvp.claim.service.documents.ClaimDocumentsServiceImpl;
import com.claims.mvp.claim.service.lifecycle.ClaimLifecycleServiceImpl;
import com.claims.mvp.claim.service.letter.ClaimLetterService;
import com.claims.mvp.claim.service.letter.ClaimLetterServiceImpl;
import com.claims.mvp.claim.service.letter.strategy.CancellationLetterStrategy;
import com.claims.mvp.claim.service.letter.strategy.DelayLetterStrategy;
import com.claims.mvp.claim.service.letter.strategy.MissedConnectionLetterStrategy;
import com.claims.mvp.claim.service.workflow.ClaimWorkflowService;
import com.claims.mvp.claim.service.workflow.ClaimWorkflowServiceImpl;
import com.claims.mvp.eligibility.strategy.CancellationEligibilityStrategy;
import com.claims.mvp.eligibility.strategy.DelayEligibilityStrategy;
import com.claims.mvp.eligibility.strategy.MissedConnectionEligibilityStrategy;
import com.claims.mvp.events.dao.EventsRepository;
import tools.jackson.databind.ObjectMapper;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private com.claims.mvp.claim.dao.BoardingDocumentsRepository boardingDocumentsRepository;

    @Mock
    private com.claims.mvp.claim.mapper.DocumentMapper documentMapper;

    private ClaimService service;

    @BeforeEach
    void setUp() {
        EligibilityService eligibilityService = new EligibilityServiceImpl(
                List.of(
                        new DelayEligibilityStrategy(),
                        new CancellationEligibilityStrategy(),
                        new MissedConnectionEligibilityStrategy()
                )
        );
        ClaimWorkflowService workflowService = new ClaimWorkflowServiceImpl();
        ClaimDocumentsService documentsService = new ClaimDocumentsServiceImpl();
        ClaimLetterService letterService = new ClaimLetterServiceImpl(
                List.of(
                        new DelayLetterStrategy(),
                        new CancellationLetterStrategy(),
                        new MissedConnectionLetterStrategy()
                )
        );

        ClaimEntityMapper entityMapper = Mappers.getMapper(ClaimEntityMapper.class);
        ClaimMapper claimMapper = Mappers.getMapper(ClaimMapper.class);

        service = new ClaimLifecycleServiceImpl(
                claimRepository,
                userRepository,
                eligibilityService,
                workflowService,
                documentsService,
                eventsRepository,
                entityMapper,
                claimMapper,
                letterService,
                new ObjectMapper()
        );
        lenient().when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(eventsRepository.save(any(ClaimEvents.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createClaim_withoutRequiredDocuments_setsDocsRequested() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        var response = service.createClaim(buildCreateRequest(List.of()));

        assertThat(response.getEligible()).isTrue();
        assertThat(response.getCompensationAmount()).isEqualTo(400);
        assertThat(response.getStatus()).isEqualTo(ClaimStatus.DOCS_REQUESTED);
    }

    @Test
    void createClaim_withRequiredDocuments_setsReadyToSubmit() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        var response = service.createClaim(buildCreateRequest(List.of(
                document("ticket-1", DocumentTypes.TICKET),
                document("boarding-pass-1", DocumentTypes.BOARDING_PASS)
        )));

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);
        assertThat(response.getDocuments()).hasSize(2);
    }

    @Test
    void updateClaimDetails_whenMissingDocumentsUploaded_promotesToReadyToSubmit() {
        Claim claim = existingClaim(ClaimStatus.DOCS_REQUESTED, List.of());
        when(claimRepository.findWithDetailsById(7L)).thenReturn(Optional.of(claim));

        UpdateClaimDetailsRequest request = new UpdateClaimDetailsRequest();
        request.setDocuments(List.of(
                document("ticket-1", DocumentTypes.TICKET),
                document("boarding-pass-1", DocumentTypes.BOARDING_PASS)
        ));

        var response = service.updateClaimDetails(7L, request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.READY_TO_SUBMIT);
        assertThat(response.getDocuments()).hasSize(2);
    }

    @Test
    void updateClaimStatus_validTransition_savesEvent() {
        Claim claim = existingClaim(ClaimStatus.READY_TO_SUBMIT, List.of(
                boardingDocument("ticket-1", DocumentTypes.TICKET),
                boardingDocument("boarding-pass-1", DocumentTypes.BOARDING_PASS)
        ));
        when(claimRepository.findWithDetailsById(7L)).thenReturn(Optional.of(claim));

        StatusChangeRequest request = new StatusChangeRequest(ClaimStatus.SUBMITTED, "sent via email");

        var response = service.transition(7L, request);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);

        ArgumentCaptor<ClaimEvents> eventCaptor = ArgumentCaptor.forClass(ClaimEvents.class);
        verify(eventsRepository).save(eventCaptor.capture());
        ClaimEvents savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getType()).isEqualTo(EventTypes.LETTER_SUBMITTED);
        assertThat(savedEvent.getPayload()).contains("\"from\":\"READY_TO_SUBMIT\"");
        assertThat(savedEvent.getPayload()).contains("\"to\":\"SUBMITTED\"");
    }

    @Test
    void updateClaimStatus_invalidTransition_throwsAndDoesNotSaveEvent() {
        Claim claim = existingClaim(ClaimStatus.NEW, List.of());
        when(claimRepository.findWithDetailsById(7L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.transition(7L, new StatusChangeRequest(ClaimStatus.PAID, "skip")) )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transition from NEW to PAID");

        verify(claimRepository, never()).save(any(Claim.class));
        verify(eventsRepository, never()).save(any(ClaimEvents.class));
    }

    @Test
    void getClaimEvents_claimMissing_throws404StyleException() {
        when(claimRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getClaimEvents(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Claim not found with id: 99");
    }

    private CreateClaimRequest buildCreateRequest(List<BoardingDocumentRequest> documents) {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setUserId(1L);
        request.setFlight(flight(1800));
        request.setIssue(delayIssue(200));
        request.setEuContext(euContext(true, true));
        request.setDocuments(documents);
        return request;
    }

    private Claim existingClaim(ClaimStatus status, List<BoardingDocuments> documents) {
        Claim claim = new Claim();
        claim.setId(7L);
        claim.setStatus(status);
        claim.setUser(user(1L));

        Flight flight = new Flight();
        flight.setClaim(claim);
        flight.setFlightNumber("LH123");
        flight.setFlightDate(LocalDate.of(2026, 3, 1));
        flight.setRouteFrom("FRA");
        flight.setRouteTo("MAD");
        flight.setAirline("Lufthansa");
        flight.setBookingRef("ABC123");
        flight.setDistanceKm(1800);
        claim.setFlight(flight);

        Issue issue = new Issue();
        issue.setClaim(claim);
        issue.setType(IssueType.DELAY);
        issue.setDelayMinutes(200);
        issue.setExtraordinaryCircumstances(false);
        claim.setIssue(issue);

        EuContext context = new EuContext();
        context.setClaim(claim);
        context.setDepartureFromEu(true);
        context.setEuCarrier(true);
        claim.setEuContext(context);

        documents.forEach(document -> document.setClaim(claim));
        claim.setDocuments(new ArrayList<>(documents));
        return claim;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Ivan Petrov");
        user.setEmail("ivan@example.com");
        return user;
    }

    private FlightRequest flight(int distanceKm) {
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

    private IssueRequest delayIssue(int delayMinutes) {
        IssueRequest issue = new IssueRequest();
        issue.setType(IssueType.DELAY);
        issue.setDelayMinutes(delayMinutes);
        issue.setExtraordinaryCircumstances(false);
        return issue;
    }

    private EuContextRequest euContext(boolean departureFromEu, boolean euCarrier) {
        EuContextRequest context = new EuContextRequest();
        context.setDepartureFromEu(departureFromEu);
        context.setEuCarrier(euCarrier);
        return context;
    }

    private BoardingDocumentRequest document(String id, DocumentTypes type) {
        BoardingDocumentRequest document = new BoardingDocumentRequest();
        document.setId(id);
        document.setType(type);
        document.setUrl("https://example.test/" + id);
        return document;
    }

    private BoardingDocuments boardingDocument(String id, DocumentTypes type) {
        BoardingDocuments document = new BoardingDocuments();
        document.setId(id);
        document.setType(type);
        document.setUrl("https://example.test/" + id);
        return document;
    }
}
