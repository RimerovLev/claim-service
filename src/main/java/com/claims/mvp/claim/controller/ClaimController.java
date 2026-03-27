package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.ClaimResponse;
import com.claims.mvp.claim.dto.CreateClaimRequest;
import com.claims.mvp.claim.dto.EvaluateClaimRequest;
import com.claims.mvp.user.dto.UserDto;
import com.claims.mvp.claim.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<ClaimResponse> evaluateClaim(@PathVariable Long id,
                                                       @Valid @RequestBody EvaluateClaimRequest request) {
        return ResponseEntity.ok(claimService.evaluateClaim(id, request));
    }

}
