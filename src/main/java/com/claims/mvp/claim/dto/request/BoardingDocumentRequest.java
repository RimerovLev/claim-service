package com.claims.mvp.claim.dto.request;

import com.claims.mvp.claim.enums.DocumentTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoardingDocumentRequest {
    private String id;
    @NotNull
    private DocumentTypes type;
    @NotBlank
    private String url;
    private OffsetDateTime uploadedAt;
}

