package com.claims.mvp.claim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
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
    private FlightDto flight;

    @NotNull
    @Valid
    private IssueDto issue;

    @Valid
    List<BoardingDocumentDto> documents;

    @NotNull
    @Valid
    private EuContextDto euContext;

}
