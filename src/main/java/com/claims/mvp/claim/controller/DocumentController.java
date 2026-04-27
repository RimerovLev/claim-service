package com.claims.mvp.claim.controller;

import com.claims.mvp.claim.dto.request.DocumentUploadRequest;
import com.claims.mvp.claim.dto.response.DocumentResponse;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.service.storage.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentStorageService documentStorageService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam Long claimId,
            @RequestParam String type,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String description) throws IOException {

        DocumentTypes documentType;
        try {
            documentType = DocumentTypes.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document type: " + type);
        }

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setClaimId(claimId);
        request.setType(DocumentTypes.valueOf(documentType.name().toUpperCase()));
        request.setFile(file);
        request.setDescription(description);

        DocumentResponse response = documentStorageService.uploadDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByClaimId(@PathVariable Long claimId) throws IOException {
        log.info("Getting documents for claim: {}", claimId);
        List<DocumentResponse> documents = documentStorageService.getDocumentsByClaimId(claimId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) throws IOException {
        DocumentResponse response = documentStorageService.downloadDocument(documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(response.getFileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(response.getResource());
    }

    @DeleteMapping("/delete/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) throws IOException {
        documentStorageService.deleteDocument(documentId);
        return ResponseEntity.ok().build();
    }
}