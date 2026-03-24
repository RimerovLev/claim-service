package com.claims.mvp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClaimRequest {
    @NotNull
    @Valid
    private UserDto user;

    @NotNull
    @Valid
    private FlightDto flight;

    @NotNull
    @Valid
    private IssueDto issue;

    @NotNull
    @Valid
    private EuContextDto euContext;


}
