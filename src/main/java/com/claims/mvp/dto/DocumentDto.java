package com.claims.mvp.dto;

import com.claims.mvp.dto.enums.DocumentTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDto {
    private String id;
    private DocumentTypes type;
    private String url;
    private OffsetDateTime uploadedAt;
}
