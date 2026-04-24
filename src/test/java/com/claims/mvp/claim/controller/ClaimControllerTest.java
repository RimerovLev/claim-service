package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.request.CloseClaimRequest;
import com.claims.mvp.claim.dto.request.CreateClaimRequest;
import com.claims.mvp.claim.dto.request.SubmitClaimRequest;
import com.claims.mvp.claim.dto.response.ClaimResponse;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
@Import(GlobalExceptionHandler.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimService claimService;

    @Test
    void createClaim_validRequest_returns201() throws Exception {
        ClaimResponse response = new ClaimResponse();
        response.setId(10L);
        response.setStatus(ClaimStatus.DOCS_REQUESTED);

        when(claimService.createClaim(any(CreateClaimRequest.class))).thenReturn(response);

        String requestBody = """
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
                  },
                  "documents": []
                }
                """;

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("DOCS_REQUESTED"));

        verify(claimService).createClaim(any(CreateClaimRequest.class));
    }

    @Test
    void createClaim_invalidRequest_returns400WithMessage() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void submitClaim_delegatesToService() throws Exception {
        ClaimResponse response = new ClaimResponse();
        response.setId(11L);
        response.setStatus(ClaimStatus.SUBMITTED);

        when(claimService.submitClaim(eq(11L), any(SubmitClaimRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/claims/11/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "submitted via email"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(claimService).submitClaim(eq(11L), any(SubmitClaimRequest.class));
    }

    @Test
    void closeClaim_delegatesToService() throws Exception {
        ClaimResponse response = new ClaimResponse();
        response.setId(12L);
        response.setStatus(ClaimStatus.CLOSED);

        when(claimService.closeClaim(eq(12L), any(CloseClaimRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/claims/12/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "claim completed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(claimService).closeClaim(eq(12L), any(CloseClaimRequest.class));
    }

    @Test
    void updateClaimDetails_validationFailure_returns400WithMessage() throws Exception {
        mockMvc.perform(patch("/api/claims/5/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": {
                                    "flightNumber": ""
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getClaimById_notFound_fromService_returns404() throws Exception {
        when(claimService.getClaimById(99L)).thenThrow(new jakarta.persistence.EntityNotFoundException("Claim not found"));

        mockMvc.perform(get("/api/claims/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Claim not found"));
    }
}
