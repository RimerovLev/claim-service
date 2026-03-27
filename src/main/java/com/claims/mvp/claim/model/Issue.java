package com.claims.mvp.claim.model;

import com.claims.mvp.claim.enums.IssueType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "issues")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueType type;
    private Integer delayMinutes;
    private Integer cancellationNoticeDays;
    @Column(nullable = false)
    private Boolean extraordinaryCircumstances;
}
