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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
/**
 * ClaimLifecycleService (application/orchestrator layer).
 *
 * Owns the claim "lifecycle" from the API perspective:
 * - createClaim / updateClaimDetails / updateClaimStatus
 * - persisting Claim and related entities
 * - writing events (ClaimEvents)
 *
 * Rule of thumb: this service orchestrates other services and repositories,
 * while delegating "pure" rules (workflow, documents, eligibility) to dedicated components.
 */
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
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));

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
    @Transactional
    public ClaimResponse updateClaimStatus(Long id, StatusChangeRequest request) {
        // Manual status change: validate transition and write ClaimEvents.
        ClaimStatus newStatus = request.getStatus();
        if (newStatus == null) {
            throw new IllegalArgumentException("Status must not be null");
        }

        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));

        workflowService.assertTransitionAllowed(claim.getStatus(), newStatus);

        ClaimEvents claimEvents = new ClaimEvents();
        claimEvents.setClaim(claim);
        claimEvents.setType(EventTypes.STATUS_CHANGED);

        String safeNote = Optional.ofNullable(request.getNote()).orElse("");
        String payload = "{\"from\":\"" + claim.getStatus() + "\",\"to\":\"" + newStatus + "\",\"note\":\"" + safeNote + "\"}";
        claimEvents.setPayload(payload);

        claim.setStatus(newStatus);

        claimRepository.save(claim);
        eventsRepository.save(claimEvents);

        return claimMapper.toResponse(claim);
    }

    @Override
    public ClaimResponse getClaimById(Long id) {
        // Fetch a single claim by id.
        return claimRepository.findById(id)
                .map(claimMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
    }

    @Override
    public Iterable<ClaimResponse> getAllClaims() {
        // Fetch claim list. For MVP we return all.
        return claimRepository.findAll().stream()
                .map(claimMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
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
    public LetterResponse getClaimLetter(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
        return claimLetterService.generateLetter(claim);
    }

    @Override
    @Transactional
    public ClaimResponse submitClaim(Long id, SubmitClaimRequest request) {
        return moveClaimEvent(id, ClaimStatus.SUBMITTED, EventTypes.LETTER_SUBMITTED, request == null ? null : request.getNote());
    }

    @Override
    @Transactional
    public ClaimResponse sendFollowUp(Long id, FollowUpRequest request) {
        return moveClaimEvent(id, ClaimStatus.FOLLOW_UP_SENT, EventTypes.FOLLOW_UP_SENT, request == null ? null : request.getNote());
    }

    @Override
    @Transactional
    public ClaimResponse approveClaim(Long id, ApproveClaimRequest request) {
        return moveClaimEvent(id, ClaimStatus.APPROVED, EventTypes.CLAIM_APPROVED, request == null ? null : request.getNote());
    }

    @Override
    @Transactional
    public ClaimResponse rejectClaim(Long id, RejectClaimRequest request) {
        return moveClaimEvent(id, ClaimStatus.REJECTED, EventTypes.CLAIM_REJECTED, request == null ? null : request.getNote());
    }

    @Override
    public ClaimResponse paidClaim(Long id, PaidClaimRequest request) {
        return moveClaimEvent(id, ClaimStatus.PAID, EventTypes.CLAIM_PAID, request == null ? null : request.getNote());
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

    private ClaimResponse moveClaimEvent(Long id,ClaimStatus targetStatus, EventTypes type, String note) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
        workflowService.assertTransitionAllowed(claim.getStatus(), targetStatus);
        ClaimEvents claimEvents = new ClaimEvents();
        claimEvents.setClaim(claim);
        claimEvents.setType(type);
        claimEvents.setPayload(note == null ? "" : note);
        claim.setStatus(targetStatus);
        claimRepository.save(claim);
        eventsRepository.save(claimEvents);
        return claimMapper.toResponse(claim);
    }
}
