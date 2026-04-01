package com.claims.mvp.events.dto;

import com.claims.mvp.claim.enums.EventTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventsResponseDto {
    private Long id;
    private EventTypes type;
    private String payload;
    private OffsetDateTime createdAt;
}
