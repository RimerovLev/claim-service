package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dto.ClaimResponse;
import com.claims.mvp.claim.dto.CreateClaimRequest;
import com.claims.mvp.claim.dto.StatusChangeRequest;
import com.claims.mvp.claim.dto.UpdateClaimDetails;
import com.claims.mvp.events.dto.EventsResponseDto;
import jakarta.validation.Valid;

import java.util.List;

public interface ClaimService {
    ClaimResponse createClaim(CreateClaimRequest request);
    ClaimResponse getClaimById(Long id);
    Iterable<ClaimResponse> getAllClaims();
    ClaimResponse updateClaimDetails(Long id, @Valid UpdateClaimDetails request);
    ClaimResponse updateClaimStatus(Long id, StatusChangeRequest request);
    List<EventsResponseDto> getClaimEvents(Long id);
}
