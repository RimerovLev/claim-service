package com.claims.mvp.eligibility.dto;

import com.claims.mvp.claim.enums.DocumentTypes;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EligibilityResult {
    private Boolean eligible;
    private Integer compensationAmount;
    private String reasonCode;
    private List<DocumentTypes> requiredDocuments;
    private String notes;
}
