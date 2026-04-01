package com.claims.mvp.claim.service.documents;

import com.claims.mvp.claim.dto.BoardingDocumentDto;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.Claim;

import java.util.List;
import java.util.Set;

public interface ClaimDocumentsService {
    List<BoardingDocuments> mapForCreate(List<BoardingDocumentDto> documentDtos, Claim claim);

    void mergeForUpdate(Claim claim, List<BoardingDocumentDto> documentDtos);

    Set<DocumentTypes> uploadedTypes(Claim claim);
}

