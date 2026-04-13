package com.claims.mvp.claim.service.documents;

import com.claims.mvp.claim.dto.request.BoardingDocumentRequest;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.Claim;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
/**
 * ClaimDocumentsService.
 *
 * Owns all claim document logic:
 * - map DTO -> entity on create
 * - merge documents on update (partial uploads)
 * - compute which document types are currently uploaded
 *
 * Important: hides the JPA orphanRemoval=true nuance — we update the managed collection in-place.
 */
public class ClaimDocumentsServiceImpl implements ClaimDocumentsService {

    @Override
    public List<BoardingDocuments> mapForCreate(List<BoardingDocumentRequest> documentDtos, Claim claim) {
        // Create-case: documents are built from scratch (nothing to merge yet).
        // null -> empty list (documents can be uploaded later).
        return Optional.ofNullable(documentDtos)
                .orElse(List.of())
                .stream()
                .map(dto -> toEntity(dto, claim, null))
                .collect(Collectors.toList());
    }

    @Override
    public void mergeForUpdate(Claim claim, List<BoardingDocumentRequest> documentDtos) {
        // Update-case: documents can arrive in parts (e.g., Ticket first, BoardingPass later).
        if (documentDtos == null) {
            return;
        }
        if (claim.getDocuments() == null) {
            claim.setDocuments(new ArrayList<>());
        }

        // Merge by DocumentTypes:
        // - if the type already exists -> update the existing entity
        // - if the type is new -> add a new entity
        Map<DocumentTypes, BoardingDocuments> byType = claim.getDocuments()
                .stream()
                .collect(Collectors.toMap(
                        BoardingDocuments::getType,
                        d -> d,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        for (BoardingDocumentRequest dto : documentDtos) {
            BoardingDocuments existing = byType.get(dto.getType());
            BoardingDocuments merged = toEntity(dto, claim, existing);
            byType.put(merged.getType(), merged);
        }

        // orphanRemoval=true: do NOT replace the collection instance (setDocuments(newList)).
        // Hibernate must see that we mutate the same managed collection.
        claim.getDocuments().clear();
        claim.getDocuments().addAll(byType.values());
    }

    @Override
    public Set<DocumentTypes> uploadedTypes(Claim claim) {
        // Returns the set of document types currently attached to the claim.
        return Optional.ofNullable(claim.getDocuments())
                .orElse(List.of())
                .stream()
                .map(BoardingDocuments::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private BoardingDocuments toEntity(BoardingDocumentRequest dto, Claim claim, BoardingDocuments target) {
        // target==null -> new document, otherwise update the existing entity.
        BoardingDocuments document = target == null ? new BoardingDocuments() : target;
        // Document id is a String: if the client did not provide one, generate a UUID.
        document.setId(dto.getId() != null
                ? dto.getId()
                : Optional.ofNullable(document.getId()).orElse(UUID.randomUUID().toString()));
        document.setClaim(claim);
        document.setType(dto.getType());
        document.setUrl(dto.getUrl());
        return document;
    }
}
