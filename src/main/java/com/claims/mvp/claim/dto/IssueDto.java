package com.claims.mvp.claim.dto;

import com.claims.mvp.claim.enums.IssueType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueDto {
    @NotNull
    private IssueType type;
    private Integer delayMinutes;
    private Integer cancellationNoticeDays;
    @NotNull
    @Getter
    private Boolean extraordinaryCircumstances;
}
