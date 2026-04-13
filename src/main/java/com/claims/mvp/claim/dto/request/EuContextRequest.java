package com.claims.mvp.claim.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EuContextRequest {
    @NotNull
    private Boolean departureFromEu;
    @NotNull
    private Boolean euCarrier;
}

