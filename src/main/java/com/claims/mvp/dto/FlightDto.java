package com.claims.mvp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FlightDto {
    @NotBlank
    private String flightNumber;
    @NotNull
    private LocalDate flightDate;
    @NotBlank
    private String routeFrom;
    @NotBlank
    private String routeTo;
    @NotBlank
    private String airline;
    @NotBlank
    private String bookingRef;

}
