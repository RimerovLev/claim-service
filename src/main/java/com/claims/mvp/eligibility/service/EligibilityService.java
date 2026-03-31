package com.claims.mvp.eligibility.service;

import com.claims.mvp.claim.dto.EuContextDto;
import com.claims.mvp.claim.dto.FlightDto;
import com.claims.mvp.claim.dto.IssueDto;
import com.claims.mvp.eligibility.dto.EligibilityResult;

public interface EligibilityService {

    EligibilityResult evaluate(IssueDto issue, FlightDto flight, EuContextDto euContext);

    int calculateCompensationAmount(Integer distanceKm);
}
