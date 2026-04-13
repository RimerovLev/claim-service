package com.claims.mvp.claim.dto.response;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimResponse {
    private Long id;
    private ClaimStatus status;
    private Boolean eligible;
    private Integer compensationAmount;
    private OffsetDateTime createdAt;
    private UserResponse user;
    private FlightResponse flight;
    private IssueResponse issue;
    private EuContextResponse euContext;
    private List<BoardingDocumentResponse> documents;
}

