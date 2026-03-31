package com.claims.mvp.claim.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateClaimDetails {
    @Valid
    private IssueDto issue;

    @Valid
    private EuContextDto euContext;

    @Valid
    private FlightDto flight;
}
