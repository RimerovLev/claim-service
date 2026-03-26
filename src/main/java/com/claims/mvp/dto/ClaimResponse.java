package com.claims.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimResponse {
    private Long id;
    private String status;
    private Boolean eligible;
    private Integer compensationAmount;
    private String flightNumber;
    private LocalDate flightDate;
    private String issueType;
    private OffsetDateTime createdAt;

}
