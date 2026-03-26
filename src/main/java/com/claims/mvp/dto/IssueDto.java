package com.claims.mvp.dto;

import com.claims.mvp.dto.enums.IssueType;
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
