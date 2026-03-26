package com.claims.mvp.service;

import com.claims.mvp.dao.ClaimRepository;
import com.claims.mvp.dto.*;
import com.claims.mvp.model.Claim;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ClaimServiceAppl implements ClaimService {
    final ClaimRepository claimRepository;
    final ModelMapper modelMapper;


    @Override
    public ClaimResponse createClaim(CreateClaimRequest request) {
        Claim claim = new Claim();
        claim.setStatus("NEW");
        claim.setEligible(false);
        claim.setCompensationAmount(0);

        claim.setFlightNumber(request.getFlight().getFlightNumber());
        claim.setFlightDate(request.getFlight().getFlightDate());
        claim.setIssueType(request.getIssue().getType().name());

        boolean eligible = isEligible(request.getIssue(), request.getEuContext());
        claim.setEligible(eligible);

        int compensationAmount = eligible ? calculateCompensationAmount(request.getEuContext().getDistanceKm()) : 0;
        claim.setCompensationAmount(compensationAmount);

        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    public ClaimResponse evaluateClaim(Long id, EvaluateClaimRequest request) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
        boolean eligible = isEligible(request.getIssue(), request.getEuContext());
        claim.setEligible(eligible);

        int compensationAmount = eligible ? calculateCompensationAmount(request.getEuContext().getDistanceKm()) : 0;
        claim.setCompensationAmount(compensationAmount);
        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    public ClaimResponse getClaimById(Long id) {
        return claimRepository.findById(id)
                .map(claim -> modelMapper.map(claim, ClaimResponse.class))
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
    }

    @Override
    public Iterable<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(claim -> modelMapper.map(claim, ClaimResponse.class))
                .collect(Collectors.toList());
    }



    private boolean isEligible(IssueDto issue, EuContextDto euContext) {

        // 1) Проверяем, подпадает ли рейс под действие EU261
        // Если рейс вылетает из ЕС ИЛИ авиакомпания европейская → рейс в зоне действия регламента
        boolean inScope = Boolean.TRUE.equals(euContext.isDepartureFromEu())
                || Boolean.TRUE.equals(euContext.isEuCarrier());

        // 2) Проверяем наличие форс‑мажора
        // Если extraordinary = true → компенсация не положена
        boolean extraordinary = Boolean.TRUE.equals(issue.isExtraordinaryCircumstances());

        // 3) Условие для задержки
        // Eligibility по задержке:
        // - тип проблемы = DELAY
        // - указано количество минут задержки
        // - задержка ≥ 180 минут (3 часа)
        boolean delayEligible = issue.getType().name().equals("DELAY")
                && issue.getDelayMinutes() != null
                && issue.getDelayMinutes() >= 180;

        // 4) Условие для отмены рейса
        // Eligibility по отмене:
        // - тип проблемы = CANCELLATION
        // - указано, за сколько дней предупредили
        // - предупреждение ≤ 14 дней до вылета
        boolean cancelEligible = issue.getType().name().equals("CANCELLATION")
                && issue.getCancellationNoticeDays() != null
                && issue.getCancellationNoticeDays() <= 14;

        // 5) Итог:
        // eligible только если:
        // - рейс в зоне EU261
        // - нет форс‑мажора
        // - есть либо задержка ≥ 3 часов, либо отмена с уведомлением ≤ 14 дней
        return inScope && !extraordinary && (delayEligible || cancelEligible);
    }


    private int calculateCompensationAmount(Integer distanceKm) {
        if(distanceKm == null) return 0;
        if(distanceKm <= 1500) return 250;
        if(distanceKm <= 3500) return 400;
        return 600;
    }
}
