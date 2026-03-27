package com.claims.mvp.claim.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "flights")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)

    private Claim claim;
    @Column(nullable = false)

    private String flightNumber;

    @Column(nullable = false)
    private LocalDate flightDate;

    @Column(nullable = false)
    private String routeFrom;

    @Column(nullable = false)
    private String routeTo;

    @Column(nullable = false)
    private String airline;

    @Column(nullable = false)
    private String bookingRef;

    @Column(nullable = false)
    private Integer distanceKm;

}
