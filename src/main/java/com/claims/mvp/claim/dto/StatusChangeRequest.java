package com.claims.mvp.claim.dto;

import com.claims.mvp.claim.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatusChangeRequest {
    @NotNull
    private ClaimStatus status;
    private String note;
}
