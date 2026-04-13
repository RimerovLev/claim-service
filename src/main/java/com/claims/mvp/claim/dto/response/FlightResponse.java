package com.claims.mvp.claim.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FlightResponse {
    private String flightNumber;
    private LocalDate flightDate;
    private String routeFrom;
    private String routeTo;
    private String airline;
    private String bookingRef;
    private Integer distanceKm;
}

