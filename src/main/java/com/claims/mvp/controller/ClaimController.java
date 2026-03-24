package com.claims.mvp.controller;

import com.claims.mvp.dto.CreateClaimRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    @PostMapping
    public ResponseEntity<String> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        return ResponseEntity.ok("Claim created for user: " + request.getUser().getFullName() + "");
    }

}
