package com.claims.mvp.claim.mapper;

import com.claims.mvp.claim.dto.response.DocumentResponse;
import com.claims.mvp.claim.model.BoardingDocuments;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")

public interface DocumentMapper {
    @Mapping(target = "claimId", source = "claim.id")
    @Mapping(target = "uploadedAt", source = "uploadedAt")
    DocumentResponse toResponse(BoardingDocuments document);
    BoardingDocuments toEntity(DocumentResponse response);
}
