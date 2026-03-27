package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dto.ClaimResponse;
import com.claims.mvp.claim.dto.CreateClaimRequest;
import com.claims.mvp.claim.dto.EvaluateClaimRequest;
import com.claims.mvp.user.dto.UserDto;
import jakarta.validation.Valid;

public interface ClaimService {
    ClaimResponse createClaim(CreateClaimRequest request);
    ClaimResponse getClaimById(Long id);
    Iterable<ClaimResponse> getAllClaims();

    ClaimResponse evaluateClaim(Long id, @Valid EvaluateClaimRequest request);

}
