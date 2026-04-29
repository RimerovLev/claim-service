package com.claims.mvp.claim.service.documents;

import com.claims.mvp.claim.dto.request.BoardingDocumentRequest;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.Claim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimDocumentsServiceImpl implements ClaimDocumentsService {

    @Override
    public List<BoardingDocuments> mapForCreate(List<BoardingDocumentRequest> documentDtos, Claim claim) {
        return Optional.ofNullable(documentDtos)
                .orElse(List.of())
                .stream()
                .map(dto -> toEntity(dto, claim, null))
                .collect(Collectors.toList());
    }

    @Override
    public void mergeForUpdate(Claim claim, List<BoardingDocumentRequest> documentDtos) {
        if (documentDtos == null) {
            return;
        }
        if (claim.getDocuments() == null) {
            claim.setDocuments(new ArrayList<>());
        }

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
            if (existing != null) {
                existing.setUrl(dto.getUrl());
            } else {
               byType.put(dto.getType(), toEntity(dto, claim, null));
            }
        }

        claim.getDocuments().clear();
        claim.getDocuments().addAll(byType.values());
    }

    @Override
    public Set<DocumentTypes> uploadedTypes(Claim claim) {
        return Optional.ofNullable(claim.getDocuments())
                .orElse(List.of())
                .stream()
                .map(BoardingDocuments::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private BoardingDocuments toEntity(BoardingDocumentRequest dto, Claim claim, BoardingDocuments target) {
        BoardingDocuments document = target == null ? new BoardingDocuments() : target;
        // Only set ID on CREATE (target == null)
        if (target == null) {
            document.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString());
        }
        // On UPDATE: keep existing ID, ignore dto.getId()
        document.setClaim(claim);
        if(dto.getType() == null){
            throw new IllegalArgumentException("Document type cannot be null");
        }
        document.setType(dto.getType());
        if(dto.getUrl() == null || dto.getUrl().isBlank()){
            throw new IllegalArgumentException("Document URL cannot be null");
        }
        document.setUrl(dto.getUrl());
        return document;
    }
}
