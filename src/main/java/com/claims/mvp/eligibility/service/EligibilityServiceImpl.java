package com.claims.mvp.eligibility.service;

import com.claims.mvp.claim.dto.BoardingDocumentDto;
import com.claims.mvp.claim.dto.EuContextDto;
import com.claims.mvp.claim.dto.FlightDto;
import com.claims.mvp.claim.dto.IssueDto;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.eligibility.dto.EligibilityResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EligibilityServiceImpl implements EligibilityService{

    @Override
    public EligibilityResult evaluate(IssueDto issue, FlightDto flight, EuContextDto euContext, List<BoardingDocumentDto> documents) {

        // 1) Проверяем, подпадает ли рейс под действие EU261 Если рейс вылетает из ЕС ИЛИ авиакомпания европейская → рейс в зоне действия регламента
        boolean inScope = Boolean.TRUE.equals(euContext.getDepartureFromEu())
                || Boolean.TRUE.equals(euContext.getEuCarrier());

        // 2) Проверяем наличие форс‑мажора Если extraordinary = true → компенсация не положена
        boolean extraordinary = Boolean.TRUE.equals(issue.getExtraordinaryCircumstances());

        // 3) Условие для задержки Eligibility по задержке: - тип проблемы = DELAY
        // - указано количество минут задержки - задержка ≥ 180 минут (3 часа)
        boolean delayEligible = issue.getType() == IssueType.DELAY
                && issue.getDelayMinutes() != null
                && issue.getDelayMinutes() >= 180;

        // 4) Условие для отмены рейса Eligibility по отмене: - тип проблемы = CANCELLATION
        // - указано, за сколько дней предупредили /- предупреждение ≤ 14 дней до вылета
        boolean cancelEligible = issue.getType() == IssueType.CANCELLATION
                && issue.getCancellationNoticeDays() != null
                && issue.getCancellationNoticeDays() <= 14;

        EligibilityResult result = new EligibilityResult();
        boolean isFlightClaim = issue.getType() == IssueType.DELAY || issue.getType() == IssueType.CANCELLATION;
        result.setRequiredDocuments(
                isFlightClaim
                        ? List.of(DocumentTypes.TICKET, DocumentTypes.BOARDING_PASS)
                        : List.of(DocumentTypes.PIR, DocumentTypes.BAG_TAG, DocumentTypes.PHOTO)
        );

        boolean eligible = inScope && !extraordinary && (delayEligible || cancelEligible);
        result.setEligible(eligible);
        result.setCompensationAmount(eligible ? calculateCompensationAmount(flight.getDistanceKm()) : 0);

        return result;
    }

    @Override
    public int calculateCompensationAmount(Integer distanceKm) {
        if(distanceKm == null) return 0;
        if(distanceKm <= 1500) return 250;
        if(distanceKm <= 3500) return 400;
        return 600;
    }

}
