package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.request.*;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.dto.response.LetterResponse;
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

    @GetMapping("/{id}/letter")
    public LetterResponse getClaimLetter(@PathVariable Long id) {
        return claimService.getClaimLetter(id);
    }

    @PostMapping("/{id}/submit")
    public ClaimResponse submitClaim(@PathVariable Long id, @RequestBody(required = false) SubmitClaimRequest request) {
        return claimService.submitClaim(id, request);
    }

    @PostMapping("/{id}/follow-up")
    public ClaimResponse sendFollowUp(@PathVariable Long id, @RequestBody FollowUpRequest request){
        return claimService.sendFollowUp(id, request);
    }

    @PostMapping("/{id}/approve")
    public ClaimResponse approveClaim(@PathVariable Long id, @RequestBody ApproveClaimRequest request){
        return claimService.approveClaim(id, request);
    }

    @PostMapping("/{id}/reject")
    public ClaimResponse rejectClaim(@PathVariable Long id, @RequestBody RejectClaimRequest request){
        return claimService.rejectClaim(id, request);
    }

    @PostMapping("/{id}/paid")
    public ClaimResponse paidClaim(@PathVariable Long id, @RequestBody PaidClaimRequest request){
        return claimService.markClaimAsPaid(id, request);
    }

    @PostMapping("/{id}/close")
    public ClaimResponse closeClaim(@PathVariable Long id, @RequestBody CloseClaimRequest request){
        return claimService.closeClaim(id, request);
    }

}
