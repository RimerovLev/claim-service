package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.*;
import com.claims.mvp.claim.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClaimControllerTest {

    private MockMvc mockMvc;

    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService() {
            @Override
            public ClaimResponse createClaim(CreateClaimRequest request) {
                if (request.getUserId() != null && request.getUserId() == 999L) {
                    throw new jakarta.persistence.EntityNotFoundException("User not found with id: 999");
                }
                ClaimResponse response = new ClaimResponse();
                response.setId(1L);
                response.setStatus(com.claims.mvp.claim.enums.ClaimStatus.NEW);
                response.setEligible(true);
                response.setCompensationAmount(400);
                response.setCreatedAt(OffsetDateTime.now());
                response.setDocuments(List.of());
                return response;
            }

            @Override
            public ClaimResponse getClaimById(Long id) {
                if (id == 999L) {
                    throw new jakarta.persistence.EntityNotFoundException("Claim not found with id: 999");
                }
                ClaimResponse response = new ClaimResponse();
                response.setId(id);
                response.setStatus(com.claims.mvp.claim.enums.ClaimStatus.NEW);
                response.setEligible(true);
                response.setCompensationAmount(400);
                response.setCreatedAt(OffsetDateTime.now());
                return response;
            }

            @Override
            public Iterable<ClaimResponse> getAllClaims() {
                return List.of();
            }

            @Override
            public ClaimResponse evaluateClaim(Long id, EvaluateClaimRequest request) {
                if (id == 999L) {
                    throw new jakarta.persistence.EntityNotFoundException("Claim not found with id: 999");
                }
                ClaimResponse response = new ClaimResponse();
                response.setId(id);
                response.setStatus(com.claims.mvp.claim.enums.ClaimStatus.NEW);
                response.setEligible(true);
                response.setCompensationAmount(400);
                response.setCreatedAt(OffsetDateTime.now());
                return response;
            }
        };
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ClaimController(claimService))
                .setControllerAdvice(new com.claims.mvp.exception.GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createClaim_returnsClaim() throws Exception {
        String body = """
                {
                  "userId": 1,
                  "flight": {
                    "flightNumber": "LH123",
                    "flightDate": "2026-03-01",
                    "routeFrom": "FRA",
                    "routeTo": "MAD",
                    "airline": "Lufthansa",
                    "bookingRef": "ABC123",
                    "distanceKm": 1800
                  },
                  "issue": {
                    "type": "DELAY",
                    "delayMinutes": 220,
                    "cancellationNoticeDays": null,
                    "extraordinaryCircumstances": false
                  },
                  "euContext": {
                    "departureFromEu": true,
                    "euCarrier": true
                  }
                }
                """;

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void getClaimById_returnsClaim() throws Exception {
        mockMvc.perform(get("/api/claims/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void getClaimById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/claims/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createClaim_invalidRequest_returns400() throws Exception {
        String body = """
                {
                  "userId": null,
                  "flight": null,
                  "issue": null,
                  "euContext": null
                }
                """;

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClaim_userNotFound_returns404() throws Exception {
        String body = """
                {
                  "userId": 999,
                  "flight": {
                    "flightNumber": "LH123",
                    "flightDate": "2026-03-01",
                    "routeFrom": "FRA",
                    "routeTo": "MAD",
                    "airline": "Lufthansa",
                    "bookingRef": "ABC123",
                    "distanceKm": 1800
                  },
                  "issue": {
                    "type": "DELAY",
                    "delayMinutes": 220,
                    "cancellationNoticeDays": null,
                    "extraordinaryCircumstances": false
                  },
                  "euContext": {
                    "departureFromEu": true,
                    "euCarrier": true
                  }
                }
                """;

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
