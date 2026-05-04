package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.request.*;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.events.dto.response.EventsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {
    private final ClaimService claimService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse createClaim(@Valid @RequestBody CreateClaimRequest request) {
        return claimService.createClaim(request);
    }

    @PostMapping("/{id}/transition")
    public ClaimResponse transitionClaim(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return claimService.transition(id, request);
    }

    @GetMapping("/{id}")
    public ClaimResponse getClaimById(@PathVariable Long id) {
        return claimService.getClaimById(id);
    }

    @GetMapping
    public Page<ClaimResponse> getAllClaims(Pageable pageable) {
        return claimService.getAllClaims(pageable);
    }

    @PatchMapping("/{id}/update")
    public ClaimResponse changeClaimDetails(@PathVariable Long id, @Valid @RequestBody UpdateClaimDetailsRequest request) {
        return claimService.updateClaimDetails(id, request);
    }

    @GetMapping("/{id}/events")
    public List<EventsResponse> getClaimEvents(@PathVariable Long id) {
        return claimService.getClaimEvents(id);
    }

    @GetMapping("/{id}/letter")
    public LetterResponse getClaimLetter(@PathVariable Long id) {
        return claimService.getClaimLetter(id);
    }

    @PostMapping("/{id}/status")
    public ClaimResponse changeClaimStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return claimService.transition(id, request);
    }


}
