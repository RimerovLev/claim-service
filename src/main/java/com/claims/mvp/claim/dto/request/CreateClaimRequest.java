package com.claims.mvp.claim.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateClaimRequest {
    @NotNull
    private Long userId;

    @NotNull
    @Valid
    private FlightRequest flight;

    @NotNull
    @Valid
    private IssueRequest issue;

    @Valid
    private List<BoardingDocumentRequest> documents;

    @NotNull
    @Valid
    private EuContextRequest euContext;
}

