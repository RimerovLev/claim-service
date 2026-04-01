package com.claims.mvp.claim.service.documents;

import com.claims.mvp.claim.dto.BoardingDocumentDto;
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
 * Отвечает за всю логику работы с документами claim:
 * - маппинг DTO -> entity при создании
 * - merge документов при update (дозагрузка частями)
 * - расчёт "какие типы документов загружены"
 *
 * Важно: здесь спрятан JPA-нюанс orphanRemoval=true — коллекцию документов обновляем in-place.
 */
public class ClaimDocumentsServiceImpl implements ClaimDocumentsService {

    @Override
    public List<BoardingDocuments> mapForCreate(List<BoardingDocumentDto> documentDtos, Claim claim) {
        // Create-case: documents are built from scratch (нет чего мержить).
        // null -> пустой список (документы могут быть загружены позже).
        return Optional.ofNullable(documentDtos)
                .orElse(List.of())
                .stream()
                .map(dto -> toEntity(dto, claim, null))
                .collect(Collectors.toList());
    }

    @Override
    public void mergeForUpdate(Claim claim, List<BoardingDocumentDto> documentDtos) {
        // Update-case: документы могут приходить частями (например, сначала Ticket, потом BoardingPass).
        if (documentDtos == null) {
            return;
        }
        if (claim.getDocuments() == null) {
            claim.setDocuments(new ArrayList<>());
        }

        // Merge по DocumentTypes:
        // - если тип уже есть -> обновляем существующий entity
        // - если типа нет -> добавляем новый entity
        Map<DocumentTypes, BoardingDocuments> byType = claim.getDocuments()
                .stream()
                .collect(Collectors.toMap(
                        BoardingDocuments::getType,
                        d -> d,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        for (BoardingDocumentDto dto : documentDtos) {
            BoardingDocuments existing = byType.get(dto.getType());
            BoardingDocuments merged = toEntity(dto, claim, existing);
            byType.put(merged.getType(), merged);
        }

        // orphanRemoval=true: нельзя заменить коллекцию целиком (setDocuments(newList)).
        // Hibernate должен видеть, что мы меняем *ту же* managed коллекцию.
        claim.getDocuments().clear();
        claim.getDocuments().addAll(byType.values());
    }

    @Override
    public Set<DocumentTypes> uploadedTypes(Claim claim) {
        // Возвращаем множество типов документов, которые сейчас загружены в claim.
        return Optional.ofNullable(claim.getDocuments())
                .orElse(List.of())
                .stream()
                .map(BoardingDocuments::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private BoardingDocuments toEntity(BoardingDocumentDto dto, Claim claim, BoardingDocuments target) {
        // target==null -> новый документ, иначе обновляем существующий.
        BoardingDocuments document = target == null ? new BoardingDocuments() : target;
        // id у документа String: если клиент не прислал id, генерим UUID (чтобы entity было идентифицируемо).
        document.setId(dto.getId() != null
                ? dto.getId()
                : Optional.ofNullable(document.getId()).orElse(UUID.randomUUID().toString()));
        document.setClaim(claim);
        document.setType(dto.getType());
        document.setUrl(dto.getUrl());
        return document;
    }
}
