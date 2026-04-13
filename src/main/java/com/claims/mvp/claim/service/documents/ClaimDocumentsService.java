package com.claims.mvp.claim.service.documents;

import com.claims.mvp.claim.dto.request.BoardingDocumentRequest;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.Claim;

import java.util.List;
import java.util.Set;

public interface ClaimDocumentsService {
    List<BoardingDocuments> mapForCreate(List<BoardingDocumentRequest> documentDtos, Claim claim);

    void mergeForUpdate(Claim claim, List<BoardingDocumentRequest> documentDtos);

    Set<DocumentTypes> uploadedTypes(Claim claim);
}
