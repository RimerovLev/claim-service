package com.claims.mvp.claim.dto.request;

import com.claims.mvp.claim.enums.IssueType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueRequest {
    @NotNull
    private IssueType type;
    private Integer delayMinutes;
    private Integer cancellationNoticeDays;
    @NotNull
    private Boolean extraordinaryCircumstances;
}

