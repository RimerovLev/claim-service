package com.claims.mvp.claim.dto.response;

import com.claims.mvp.claim.enums.DocumentTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoardingDocumentResponse {
    private String id;
    private DocumentTypes type;
    private String url;
    private OffsetDateTime uploadedAt;
}

