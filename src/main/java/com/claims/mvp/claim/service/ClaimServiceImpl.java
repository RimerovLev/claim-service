package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dao.ClaimRepository;
import com.claims.mvp.claim.enums.EventTypes;
import com.claims.mvp.eligibility.dto.EligibilityResult;
import com.claims.mvp.eligibility.service.EligibilityService;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.claim.dto.*;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.model.*;
import com.claims.mvp.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ClaimServiceImpl implements ClaimService {
    final ClaimRepository claimRepository;
    final UserRepository userRepository;
    final ModelMapper modelMapper;
    final EligibilityService eligibilityService;
    final EventsRepository eventsRepository;

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED_TRANSITIONS = Map.of(
            ClaimStatus.NEW, Set.of(ClaimStatus.DOCS_REQUESTED, ClaimStatus.READY_TO_SUBMIT),
            ClaimStatus.DOCS_REQUESTED, Set.of(ClaimStatus.READY_TO_SUBMIT),
            ClaimStatus.READY_TO_SUBMIT, Set.of(ClaimStatus.SUBMITTED),
            ClaimStatus.SUBMITTED, Set.of(ClaimStatus.FOLLOW_UP_SENT, ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.FOLLOW_UP_SENT, Set.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.APPROVED, Set.of(ClaimStatus.PAID),
            ClaimStatus.REJECTED, Set.of(ClaimStatus.CLOSED),
            ClaimStatus.PAID, Set.of(ClaimStatus.CLOSED)
    );

    private boolean isTransitionAllowed(ClaimStatus from, ClaimStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
    @Transactional
    public ClaimResponse updateClaimStatus(Long id, StatusChangeRequest request) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
        if(!isTransitionAllowed(claim.getStatus(), request.getStatus())) {
            throw new IllegalArgumentException("Transition from " + claim.getStatus() + " to " + request.getStatus() + " is not allowed");
        }
        claim.setStatus(request.getStatus());
        claimRepository.save(claim);

        ClaimEvents claimEvents = new ClaimEvents();
        claimEvents.setClaim(claim);
        claimEvents.setType(EventTypes.STATUS_CHANGED);
        String payload = (request.getNote() == null || request.getNote().isBlank() ? "{}" : request.getNote());
        claimEvents.setPayload(payload);
        eventsRepository.save(claimEvents);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request) {
        Claim claim = new Claim();
        User user = userRepository.findById(request.getUserId()).orElseThrow(
                () -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
        claim.setUser(user);
        claim.setStatus(ClaimStatus.NEW);

        EligibilityResult result = eligibilityService.evaluate(request.getIssue(),
                request.getFlight(), request.getEuContext());
        claim.setEligible(result.getEligible());
        claim.setCompensationAmount(result.getCompensationAmount());

        Flight flight = modelMapper.map(request.getFlight(), Flight.class);
        flight.setClaim(claim);
        claim.setFlight(flight);

        EuContext euContext = modelMapper.map(request.getEuContext(), EuContext.class);
        euContext.setClaim(claim);
        claim.setEuContext(euContext);

        List<Document> documents = Optional.ofNullable(request.getDocuments())
                .orElse(List.of())
                .stream()
                .map(d -> {
                    Document doc = modelMapper.map(d, Document.class);
                    doc.setClaim(claim);
                    return doc;
                })
                .toList();
        claim.setDocuments(documents);

        Issue issue = modelMapper.map(request.getIssue(), Issue.class);
        issue.setClaim(claim);
        claim.setIssue(issue);

        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    @Transactional
    public ClaimResponse updateClaimDetails(Long id, UpdateClaimDetails request) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));

        if(request.getFlight() != null) {
            if (claim.getFlight() == null){
                Flight flight = new Flight();
                flight.setClaim(claim);
                claim.setFlight(flight);
            }
            modelMapper.map(request.getFlight(), claim.getFlight());
        }
        if(request.getIssue() != null) {
            if (claim.getIssue() == null){
                Issue issue = new Issue();
                issue.setClaim(claim);
                claim.setIssue(issue);
            }
            modelMapper.map(request.getIssue(), claim.getIssue());
        }
        if(request.getEuContext() != null) {
            if (claim.getEuContext() == null){
                EuContext euContext = new EuContext();
                euContext.setClaim(claim);
                claim.setEuContext(euContext);
            }
            modelMapper.map(request.getEuContext(), claim.getEuContext());
        }
        if (claim.getFlight() == null || claim.getIssue() == null || claim.getEuContext() == null){
            throw new IllegalArgumentException("Claim details incomplete");
        }
        EligibilityResult result = eligibilityService.evaluate(
                modelMapper.map(claim.getIssue(), IssueDto.class),
                modelMapper.map(claim.getFlight(), FlightDto.class),
                modelMapper.map(claim.getEuContext(), EuContextDto.class)
        );

        claim.setEligible(result.getEligible());
        claim.setCompensationAmount(result.getCompensationAmount());

        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }


    @Override
    public ClaimResponse getClaimById(Long id) {
        return claimRepository.findById(id)
                .map(claim -> modelMapper.map(claim, ClaimResponse.class))
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
    }

    @Override
    public Iterable<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(claim -> modelMapper.map(claim, ClaimResponse.class))
                .collect(Collectors.toList());
    }

}
