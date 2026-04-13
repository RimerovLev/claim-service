package com.claims.mvp.claim.mapper;

import com.claims.mvp.claim.dto.request.EuContextRequest;
import com.claims.mvp.claim.dto.request.FlightRequest;
import com.claims.mvp.claim.dto.request.IssueRequest;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClaimEntityMapper {
    // Request -> new entity (create)
    Flight toEntity(FlightRequest request);
    Issue toEntity(IssueRequest request);
    EuContext toEntity(EuContextRequest request);

    // Request -> existing entity (patch/update)
    void update(FlightRequest request, @MappingTarget Flight target);
    void update(IssueRequest request, @MappingTarget Issue target);
    void update(EuContextRequest request, @MappingTarget EuContext target);
}

