package com.claims.mvp.events.dto.response;

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
public class EventsResponse {
    private Long id;
    private EventTypes type;
    private String payload;
    private OffsetDateTime createdAt;
}

