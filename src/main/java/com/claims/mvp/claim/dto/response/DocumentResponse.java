package com.claims.mvp.claim.dto.response;

import com.claims.mvp.claim.enums.DocumentTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.Resource;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponse {
    private String id;
    private Long claimId;
    private DocumentTypes type;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String description;
    private OffsetDateTime uploadedAt;
    private Resource resource;
}
