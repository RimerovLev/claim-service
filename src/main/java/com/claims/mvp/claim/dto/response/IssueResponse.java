package com.claims.mvp.claim.dto.response;

import com.claims.mvp.claim.enums.IssueType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueResponse {
    private IssueType type;
    private Integer delayMinutes;
    private Integer cancellationNoticeDays;
    private Boolean extraordinaryCircumstances;
}

