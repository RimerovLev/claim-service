package com.claims.mvp.model;

import com.claims.mvp.dto.EventTypes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "claim_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimEvents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventTypes type;
    private String payload;
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
