package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dto.request.*;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.events.dto.response.EventsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClaimService {
    ClaimResponse createClaim(CreateClaimRequest request);
    ClaimResponse getClaimById(Long id);
    Page<ClaimResponse> getAllClaims(Pageable pageable);
    ClaimResponse updateClaimDetails(Long id, UpdateClaimDetailsRequest request);
    List<EventsResponse> getClaimEvents(Long id);
    LetterResponse getClaimLetter(Long id);
    ClaimResponse transition(Long id, StatusChangeRequest request);

}
