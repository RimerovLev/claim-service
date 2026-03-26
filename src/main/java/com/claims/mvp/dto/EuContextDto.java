package com.claims.mvp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EuContextDto {
    @NotNull
    private Boolean departureFromEu;
    @NotNull
    private Boolean euCarrier;
}
