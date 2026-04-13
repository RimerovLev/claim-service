package com.claims.mvp.claim.mapper;

import com.claims.mvp.claim.dto.response.*;
import com.claims.mvp.claim.model.*;
import com.claims.mvp.events.dto.response.EventsResponse;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.user.dto.response.UserResponse;
import com.claims.mvp.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClaimMapper {
    // Entity -> API response
    ClaimResponse toResponse(Claim claim);
    UserResponse toResponse(User user);
    FlightResponse toResponse(Flight flight);
    IssueResponse toResponse(Issue issue);
    EuContextResponse toResponse(EuContext euContext);
    BoardingDocumentResponse toResponse(BoardingDocuments document);

    EventsResponse toResponse(ClaimEvents event);
}

