package com.claims.mvp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EuContextDto {
    private boolean departureFromEu;
    private boolean euCarrier;
    private Integer distanceKm;
}
