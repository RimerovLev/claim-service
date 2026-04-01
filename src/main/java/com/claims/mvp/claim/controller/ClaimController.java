package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.ClaimResponse;
import com.claims.mvp.claim.dto.CreateClaimRequest;
import com.claims.mvp.claim.dto.StatusChangeRequest;
import com.claims.mvp.claim.dto.UpdateClaimDetails;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.events.dto.EventsResponseDto;
import com.claims.mvp.events.model.ClaimEvents;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {
    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        return ResponseEntity.ok(claimService.createClaim(request));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }
    @GetMapping
    public ResponseEntity<Iterable<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @PatchMapping("/{id}/update")
    public ClaimResponse changeClaimDetails(@PathVariable Long id, @Valid @RequestBody UpdateClaimDetails request) {
        return  claimService.updateClaimDetails(id, request);
    }
    @PostMapping("/{id}/status")
    public ClaimResponse updateClaimStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return claimService.updateClaimStatus(id, request);
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<EventsResponseDto>> getClaimEvents(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimEvents(id));
    }


}
