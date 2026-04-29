package com.claims.mvp.claim.model;

import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "claims")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;
    private Boolean eligible;
    private Integer compensationAmount;
    private OffsetDateTime createdAt;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private Flight flight;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private EuContext euContext;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardingDocuments> documents;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private Issue issue;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimEvents> events;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
