package com.claims.mvp.claim.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import com.claims.mvp.claim.enums.DocumentTypes;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploadRequest {
    @NotNull
    private Long claimId;
    @NotNull
    private DocumentTypes type;
    @NotNull
    private MultipartFile file;
    private String description;
}
