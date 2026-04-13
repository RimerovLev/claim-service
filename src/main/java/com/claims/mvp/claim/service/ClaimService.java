package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dto.request.CreateClaimRequest;
import com.claims.mvp.claim.dto.request.StatusChangeRequest;
import com.claims.mvp.claim.dto.request.UpdateClaimDetailsRequest;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.events.dto.response.EventsResponse;

import java.util.List;

public interface ClaimService {
    ClaimResponse createClaim(CreateClaimRequest request);
    ClaimResponse getClaimById(Long id);
    Iterable<ClaimResponse> getAllClaims();
    ClaimResponse updateClaimDetails(Long id, UpdateClaimDetailsRequest request);
    ClaimResponse updateClaimStatus(Long id, StatusChangeRequest request);
    List<EventsResponse> getClaimEvents(Long id);
}
