package com.claims.mvp.claim.dto;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.user.dto.UserDto;
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
    private UserDto user;
    private FlightDto flight;
    private IssueDto issue;
    private EuContextDto euContext;
    private List<DocumentDto> documents;

}
