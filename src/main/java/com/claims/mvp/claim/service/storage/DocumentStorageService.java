package com.claims.mvp.claim.service.storage;

import com.claims.mvp.claim.dto.request.DocumentUploadRequest;
import com.claims.mvp.claim.dto.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentStorageService {
    DocumentResponse uploadDocument(DocumentUploadRequest request) throws IOException;
    DocumentResponse getDocument(String id) throws IOException;
    List<DocumentResponse> getDocumentsByClaimId(Long claimId) throws IOException;
    void deleteDocument(String id) throws IOException;
    void validateFile(MultipartFile file) throws IOException;
    DocumentResponse downloadDocument(String documentId) throws IOException;
}
