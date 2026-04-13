package com.claims.mvp.claim.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateClaimDetailsRequest {
    @Valid
    private IssueRequest issue;

    @Valid
    private EuContextRequest euContext;

    @Valid
    private FlightRequest flight;

    @Valid
    private List<BoardingDocumentRequest> documents;
}

