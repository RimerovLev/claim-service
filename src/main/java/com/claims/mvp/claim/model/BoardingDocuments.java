package com.claims.mvp.claim.model;

import com.claims.mvp.claim.enums.DocumentTypes;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoardingDocuments {
    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentTypes type;

    @Column(nullable = false)
    private String url;

    @Column
    private String description;

    @Column
    private String fileName;

    @Column
    private Long fileSize;

    @Column
    private String mimeType;

    @Column
    private String storageKey;

    @Column
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;
}