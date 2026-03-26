package com.claims.mvp.service;

import com.claims.mvp.dto.ClaimResponse;
import com.claims.mvp.dto.CreateClaimRequest;
import com.claims.mvp.dto.EvaluateClaimRequest;
import jakarta.validation.Valid;

public interface ClaimService {
    ClaimResponse createClaim(CreateClaimRequest request);
    ClaimResponse getClaimById(Long id);
    Iterable<ClaimResponse> getAllClaims();

    ClaimResponse evaluateClaim(Long id, @Valid EvaluateClaimRequest request);
}
