package com.claims.mvp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class EvaluateClaimRequest {
    @NotNull
    @Valid
    private IssueDto issue;

    @NotNull
    @Valid
    private EuContextDto euContext;
}
