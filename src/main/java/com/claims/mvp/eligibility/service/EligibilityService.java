package com.claims.mvp.eligibility.service;

import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.eligibility.dto.response.EligibilityResult;

import java.util.List;

public interface EligibilityService {

    EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents);
    int calculateCompensationAmount(Integer distanceKm);
}
