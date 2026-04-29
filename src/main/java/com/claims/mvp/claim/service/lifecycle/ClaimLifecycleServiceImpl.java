package com.claims.mvp.claim.service.lifecycle;

import com.claims.mvp.claim.dao.ClaimRepository;
import com.claims.mvp.claim.dto.request.*;
import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.mapper.ClaimEntityMapper;
import com.claims.mvp.claim.mapper.ClaimMapper;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.EventTypes;
import com.claims.mvp.claim.model.*;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.claim.service.documents.ClaimDocumentsService;
import com.claims.mvp.claim.service.letter.ClaimLetterService;
import com.claims.mvp.claim.service.workflow.ClaimWorkflowService;
import com.claims.mvp.eligibility.dto.response.EligibilityResult;
import com.claims.mvp.eligibility.service.EligibilityService;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.events.dto.response.EventsResponse;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ClaimLifecycleService (application/orchestrator layer).
 * <p>
 * Owns the claim "lifecycle" from the API perspective:
 * <ul>
 *   <li>{@code createClaim} — initial intake; runs eligibility and assigns the
 *       pre-submit status based on uploaded documents.</li>
 *   <li>{@code updateClaimDetails} — partial update of flight / issue / EU context
 *       / documents. Forbidden after SUBMITTED. Re-runs eligibility on every change.</li>
 *   <li>{@code transition} — single FSM transition endpoint. Validates against
 *       the workflow's allowed-transitions table and writes a typed
 *       {@link com.claims.mvp.events.model.ClaimEvents} entry.</li>
 *   <li>read-only lookups: {@code getClaimById}, {@code getAllClaims},
 *       {@code getClaimEvents}, {@code getClaimLetter}.</li>
 * </ul>
 * <p>
 * Rule of thumb: this service orchestrates other services and repositories,
 * while delegating pure rules to dedicated components:
 * <ul>
 *   <li>{@link com.claims.mvp.claim.service.workflow.ClaimWorkflowService}
 *       — FSM transitions and event-type lookup.</li>
 *   <li>{@link com.claims.mvp.eligibility.service.EligibilityService}
 *       — pure rule engine (no DB access).</li>
 *   <li>{@link com.claims.mvp.claim.service.documents.ClaimDocumentsService}
 *       — document merge / mapping logic.</li>
 *   <li>{@link com.claims.mvp.claim.service.letter.ClaimLetterService}
 *       — claim letter generation.</li>
 * </ul>
 */
@RequiredArgsConstructor
@Service

public class ClaimLifecycleServiceImpl implements ClaimService {
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final EligibilityService eligibilityService;
    private final ClaimWorkflowService workflowService;
    private final ClaimDocumentsService documentsService;
    private final EventsRepository eventsRepository;
    private final ClaimEntityMapper claimEntityMapper;
    private final ClaimMapper claimMapper;
    private final ClaimLetterService claimLetterService;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request) {
        // Create a new claim and populate "source" data (user/flight/issue/euContext/documents).
        // Then, in one step, recalculate derived fields (eligible/compensation/status).
        Claim claim = new Claim();

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
        claim.setUser(user);
        claim.setStatus(ClaimStatus.NEW);

        Flight flight = claimEntityMapper.toEntity(request.getFlight());
        flight.setClaim(claim);
        claim.setFlight(flight);

        EuContext euContext = claimEntityMapper.toEntity(request.getEuContext());
        euContext.setClaim(claim);
        claim.setEuContext(euContext);

        Issue issue = claimEntityMapper.toEntity(request.getIssue());
        issue.setClaim(claim);
        claim.setIssue(issue);

        claim.setDocuments(documentsService.mapForCreate(request.getDocuments(), claim));

        // Recalculate derived fields in one place to keep create/update consistent.
        recalcDerivedFields(claim);

        claimRepository.save(claim);
        return claimMapper.toResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse updateClaimDetails(Long id, UpdateClaimDetailsRequest request) {
        // Partial update of claim details:
        // if a block is missing (null) we keep the existing data unchanged.
        Claim claim = claimRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));


        workflowService.assertEditable(claim.getStatus());

        if (request.getFlight() != null) {
            // Update the existing Flight in-place to avoid creating extra rows.
            if (claim.getFlight() == null) {
                Flight flight = new Flight();
                flight.setClaim(claim);
                claim.setFlight(flight);
            }
            claimEntityMapper.update(request.getFlight(), claim.getFlight());
            claim.getFlight().setClaim(claim);
        }
        if (request.getIssue() != null) {
            // Same approach for Issue.
            if (claim.getIssue() == null) {
                Issue issue = new Issue();
                issue.setClaim(claim);
                claim.setIssue(issue);
            }
            claimEntityMapper.update(request.getIssue(), claim.getIssue());
            claim.getIssue().setClaim(claim);
        }
        if (request.getEuContext() != null) {
            // Same approach for EuContext.
            if (claim.getEuContext() == null) {
                EuContext context = new EuContext();
                context.setClaim(claim);
                claim.setEuContext(context);
            }
            claimEntityMapper.update(request.getEuContext(), claim.getEuContext());
            claim.getEuContext().setClaim(claim);
        }
        if (request.getDocuments() != null) {
            // Documents can be uploaded later — merge by types, see ClaimDocumentsService.
            documentsService.mergeForUpdate(claim, request.getDocuments());
        }

        if (claim.getFlight() == null || claim.getIssue() == null || claim.getEuContext() == null) {
            throw new IllegalArgumentException("Claim details incomplete");
        }

        // After updating details, always recalculate derived fields (eligible/compensation/status).
        recalcDerivedFields(claim);

        claimRepository.save(claim);
        return claimMapper.toResponse(claim);
    }

    @Override
    public ClaimResponse transition(Long id, StatusChangeRequest request) {
        ClaimStatus targetStatus = request.getStatus();
        if(targetStatus == null){
            throw new IllegalArgumentException("Status must not be null");
        }
        Claim claim = claimRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));

        workflowService.assertTransitionAllowed(claim.getStatus(), targetStatus);

        EventTypes eventType = workflowService.eventType(targetStatus);

        ClaimEvents claimEvents = new ClaimEvents();
        claimEvents.setClaim(claim);
        claimEvents.setType(eventType);
        claimEvents.setPayload(buildTransitionPayload(claim.getStatus(), targetStatus, request.getNote()));

        claim.setStatus(targetStatus);
        claimRepository.save(claim);
        eventsRepository.save(claimEvents);
        return claimMapper.toResponse(claim);
    }


    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long id) {
        // Fetch a single claim by id.
        return claimRepository.findWithDetailsById(id)
                .map(claimMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimResponse> getAllClaims(Pageable pageable) {
        // Fetch claim list. For MVP we return all.
        return claimRepository.findAll(pageable).map(claimMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventsResponse> getClaimEvents(Long id) {
        // Claim events. First validate claim exists to return 404 instead of an empty list.
        if (!claimRepository.existsById(id)) {
            throw new EntityNotFoundException("Claim not found with id: " + id);
        }
        return eventsRepository.findByClaimIdOrderByCreatedAtDesc(id).stream()
                .map(claimMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LetterResponse getClaimLetter(Long id) {
        Claim claim = claimRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
        return claimLetterService.generateLetter(claim);
    }


    private void recalcDerivedFields(Claim claim) {
        // Centralized recalculation of derived fields to keep create/update consistent:
        // - eligibilityService decides eligible/compensation and which documents are required
        // - documentsService provides which documents are actually uploaded
        // - workflowService decides how to update status in the pre-submit stage
        EligibilityResult eligibilityResult = eligibilityService.evaluate(
                claim.getIssue(),
                claim.getFlight(),
                claim.getEuContext(),
                Optional.ofNullable(claim.getDocuments()).orElse(List.of())
        );

        claim.setEligible(eligibilityResult.getEligible());
        claim.setCompensationAmount(eligibilityResult.getCompensationAmount());

        Set<DocumentTypes> required = new HashSet<>(eligibilityResult.getRequiredDocuments());
        Set<DocumentTypes> uploaded = documentsService.uploadedTypes(claim);

        boolean hasAllRequired = uploaded.containsAll(required);
        claim.setStatus(workflowService.autoPreSubmitStatus(claim.getStatus(), hasAllRequired));
    }

    private String buildTransitionPayload(ClaimStatus from, ClaimStatus to, String note) {
        return objectMapper.writeValueAsString(Map.of(
                "from", from.name(),
                "to", to.name(),
                "note", note == null ? "" : note
        ));
    }
}
