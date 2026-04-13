package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.request.CreateClaimRequest;
import com.claims.mvp.claim.dto.request.StatusChangeRequest;
import com.claims.mvp.claim.dto.request.UpdateClaimDetailsRequest;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.events.dto.response.EventsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{id}")
    public ClaimResponse getClaimById(@PathVariable Long id) {
        return claimService.getClaimById(id);
    }

    @GetMapping
    public Iterable<ClaimResponse> getAllClaims() {
        return claimService.getAllClaims();
    }

    @PatchMapping("/{id}/update")
    public ClaimResponse changeClaimDetails(@PathVariable Long id, @Valid @RequestBody UpdateClaimDetailsRequest request) {
        return  claimService.updateClaimDetails(id, request);
    }
    @PostMapping("/{id}/status")
    public ClaimResponse updateClaimStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return claimService.updateClaimStatus(id, request);
    }

    @GetMapping("/{id}/events")
    public List<EventsResponse> getClaimEvents(@PathVariable Long id) {
        return claimService.getClaimEvents(id);
    }


}
