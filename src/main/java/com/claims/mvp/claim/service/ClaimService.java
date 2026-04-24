package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dto.request.*;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.events.dto.response.EventsResponse;

import java.util.List;

public interface ClaimService {
    ClaimResponse createClaim(CreateClaimRequest request);
    ClaimResponse getClaimById(Long id);
    Iterable<ClaimResponse> getAllClaims();
    ClaimResponse updateClaimDetails(Long id, UpdateClaimDetailsRequest request);
    ClaimResponse updateClaimStatus(Long id, StatusChangeRequest request);
    List<EventsResponse> getClaimEvents(Long id);
    LetterResponse getClaimLetter(Long id);
    ClaimResponse submitClaim(Long id, SubmitClaimRequest request);
    ClaimResponse sendFollowUp(Long id, FollowUpRequest request);
    ClaimResponse approveClaim(Long id, ApproveClaimRequest request);
    ClaimResponse rejectClaim(Long id, RejectClaimRequest request);
    ClaimResponse markClaimAsPaid(Long id, PaidClaimRequest request);
    ClaimResponse closeClaim(Long id, CloseClaimRequest request);
}
