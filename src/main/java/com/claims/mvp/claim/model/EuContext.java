package com.claims.mvp.claim.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eu_context")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EuContext {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;
    @Column(nullable = false)
    private Boolean departureFromEu;
    @Column(nullable = false)
    private Boolean euCarrier;
}
