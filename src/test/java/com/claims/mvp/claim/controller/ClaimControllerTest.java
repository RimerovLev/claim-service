package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.ClaimResponse;
import com.claims.mvp.claim.dto.CreateClaimRequest;
import com.claims.mvp.claim.dto.StatusChangeRequest;
import com.claims.mvp.claim.dto.UpdateClaimDetails;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.events.dto.EventsResponseDto;
import com.claims.mvp.exception.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClaimControllerTest {

    private MockMvc mockMvc;

    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = mock(ClaimService.class);

        when(claimService.createClaim(any(CreateClaimRequest.class))).thenReturn(response(1L, ClaimStatus.DOCS_REQUESTED));
        when(claimService.getClaimById(7L)).thenReturn(response(7L, ClaimStatus.NEW));
        when(claimService.getClaimById(999L)).thenThrow(new EntityNotFoundException("Claim not found with id: 999"));
        when(claimService.updateClaimDetails(eq(7L), any(UpdateClaimDetails.class))).thenReturn(response(7L, ClaimStatus.READY_TO_SUBMIT));
        when(claimService.updateClaimStatus(eq(7L), any(StatusChangeRequest.class))).thenReturn(response(7L, ClaimStatus.SUBMITTED));
        when(claimService.getClaimEvents(7L)).thenReturn(List.of(new EventsResponseDto(5L, com.claims.mvp.claim.enums.EventTypes.STATUS_CHANGED, "{\"from\":\"READY_TO_SUBMIT\",\"to\":\"SUBMITTED\"}", OffsetDateTime.now())));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ClaimController(claimService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createClaim_returnsClaim() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DOCS_REQUESTED"));
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
    void getClaimById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/claims/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateClaimDetails_returnsUpdatedClaim() throws Exception {
        String body = """
                {
                  "documents": [
                    {
                      "id": "ticket-1",
                      "type": "TICKET",
                      "url": "https://example.test/ticket-1"
                    },
                    {
                      "id": "boarding-pass-1",
                      "type": "BOARDING_PASS",
                      "url": "https://example.test/boarding-pass-1"
                    }
                  ]
                }
                """;

        mockMvc.perform(patch("/api/claims/7/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_TO_SUBMIT"));
    }

    @Test
    void updateClaimStatus_returnsUpdatedClaim() throws Exception {
        String body = """
                {
                  "status": "SUBMITTED",
                  "note": "sent via email"
                }
                """;

        mockMvc.perform(post("/api/claims/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void getClaimEvents_returnsEvents() throws Exception {
        mockMvc.perform(get("/api/claims/7/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].type").value("STATUS_CHANGED"));
    }

    private ClaimResponse response(Long id, ClaimStatus status) {
        ClaimResponse response = new ClaimResponse();
        response.setId(id);
        response.setStatus(status);
        response.setEligible(true);
        response.setCompensationAmount(400);
        response.setCreatedAt(OffsetDateTime.now());
        response.setDocuments(List.of());
        return response;
    }

    private String validCreateBody() {
        return """
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
                    "extraordinaryCircumstances": false
                  },
                  "euContext": {
                    "departureFromEu": true,
                    "euCarrier": true
                  }
                }
                """;
    }
}
