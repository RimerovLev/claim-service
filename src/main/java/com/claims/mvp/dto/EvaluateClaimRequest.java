package com.claims.mvp.dto;

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
public class EvaluateClaimRequest {
    @NotNull
    @Valid
    private IssueDto issue;

    @NotNull
    @Valid
    private EuContextDto euContext;
}
