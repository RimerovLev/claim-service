package com.claims.mvp.claim.service.lifecycle;

import com.claims.mvp.claim.dao.ClaimRepository;
import com.claims.mvp.claim.dto.*;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.EventTypes;
import com.claims.mvp.claim.model.*;
import com.claims.mvp.claim.service.ClaimService;
import com.claims.mvp.claim.service.documents.ClaimDocumentsService;
import com.claims.mvp.claim.service.workflow.ClaimWorkflowService;
import com.claims.mvp.eligibility.dto.EligibilityResult;
import com.claims.mvp.eligibility.service.EligibilityService;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.events.dto.EventsResponseDto;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
/**
 * ClaimLifecycleService (application/orchestrator layer).
 *
 * Отвечает за "жизненный цикл" Claim с точки зрения API:
 * - createClaim / updateClaimDetails / updateClaimStatus
 * - сохранение Claim и связанных сущностей
 * - запись событий (ClaimEvents)
 *
 * Важное правило: этот сервис оркестрирует другие сервисы и репозитории,
 * а "чистые" правила (workflow, документы, eligibility) делегирует в отдельные компоненты.
 */
public class ClaimLifecycleServiceImpl implements ClaimService {
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final EligibilityService eligibilityService;
    private final ClaimWorkflowService workflowService;
    private final ClaimDocumentsService documentsService;
    private final EventsRepository eventsRepository;

    @Override
    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request) {
        // Создаём новый claim и заполняем "сырьевые" данные (user/flight/issue/euContext/documents).
        // Затем одним шагом пересчитываем derived поля (eligible/compensation/status).
        Claim claim = new Claim();

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
        claim.setUser(user);
        claim.setStatus(ClaimStatus.NEW);

        Flight flight = modelMapper.map(request.getFlight(), Flight.class);
        flight.setClaim(claim);
        claim.setFlight(flight);

        EuContext euContext = modelMapper.map(request.getEuContext(), EuContext.class);
        euContext.setClaim(claim);
        claim.setEuContext(euContext);

        Issue issue = modelMapper.map(request.getIssue(), Issue.class);
        issue.setClaim(claim);
        claim.setIssue(issue);

        claim.setDocuments(documentsService.mapForCreate(request.getDocuments(), claim));

        // Пересчёт derived полей в одном месте для консистентности create/update.
        recalcDerivedFields(claim);

        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    @Transactional
    public ClaimResponse updateClaimDetails(Long id, UpdateClaimDetails request) {
        // Частичное обновление деталей claim:
        // если какой-то блок не пришёл (null) — не трогаем существующие данные.
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));

        if (request.getFlight() != null) {
            // Обновляем существующий Flight in-place, чтобы Hibernate не создавал лишних записей.
            if (claim.getFlight() == null) {
                Flight flight = new Flight();
                flight.setClaim(claim);
                claim.setFlight(flight);
            }
            modelMapper.map(request.getFlight(), claim.getFlight());
            claim.getFlight().setClaim(claim);
        }
        if (request.getIssue() != null) {
            // Аналогично для Issue.
            if (claim.getIssue() == null) {
                Issue issue = new Issue();
                issue.setClaim(claim);
                claim.setIssue(issue);
            }
            modelMapper.map(request.getIssue(), claim.getIssue());
            claim.getIssue().setClaim(claim);
        }
        if (request.getEuContext() != null) {
            // Аналогично для EuContext.
            if (claim.getEuContext() == null) {
                EuContext context = new EuContext();
                context.setClaim(claim);
                claim.setEuContext(context);
            }
            modelMapper.map(request.getEuContext(), claim.getEuContext());
            claim.getEuContext().setClaim(claim);
        }
        if (request.getDocuments() != null) {
            // Документы можно "дозагружать" — мержим по типам, см. ClaimDocumentsService.
            documentsService.mergeForUpdate(claim, request.getDocuments());
        }

        if (claim.getFlight() == null || claim.getIssue() == null || claim.getEuContext() == null) {
            throw new IllegalArgumentException("Claim details incomplete");
        }

        // После апдейта деталей обязательно пересчитываем derived поля (eligible/compensation/status).
        recalcDerivedFields(claim);

        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    @Transactional
    public ClaimResponse updateClaimStatus(Long id, StatusChangeRequest request) {
        // Ручная смена статуса: проверяем допустимость перехода и пишем ClaimEvents.
        ClaimStatus newStatus = request.getStatus();
        if (newStatus == null) {
            throw new IllegalArgumentException("Status must not be null");
        }

        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));

        workflowService.assertTransitionAllowed(claim.getStatus(), newStatus);

        ClaimEvents claimEvents = new ClaimEvents();
        claimEvents.setClaim(claim);
        claimEvents.setType(EventTypes.STATUS_CHANGED);

        String safeNote = Optional.ofNullable(request.getNote()).orElse("");
        String payload = "{\"from\":\"" + claim.getStatus() + "\",\"to\":\"" + newStatus + "\",\"note\":\"" + safeNote + "\"}";
        claimEvents.setPayload(payload);

        claim.setStatus(newStatus);

        claimRepository.save(claim);
        eventsRepository.save(claimEvents);

        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    public ClaimResponse getClaimById(Long id) {
        // Получение одного claim по id.
        return claimRepository.findById(id)
                .map(claim -> modelMapper.map(claim, ClaimResponse.class))
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
    }

    @Override
    public Iterable<ClaimResponse> getAllClaims() {
        // Получение списка claim. Для MVP отдаём все.
        return claimRepository.findAll().stream()
                .map(claim -> modelMapper.map(claim, ClaimResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventsResponseDto> getClaimEvents(Long id) {
        // События по claim. Сначала проверяем существование claim, чтобы возвращать 404, а не пустой список.
        if (!claimRepository.existsById(id)) {
            throw new EntityNotFoundException("Claim not found with id: " + id);
        }
        return eventsRepository.findByClaimIdOrderByCreatedAtDesc(id).stream()
                .map(event -> modelMapper.map(event, EventsResponseDto.class))
                .collect(Collectors.toList());
    }

    private void recalcDerivedFields(Claim claim) {
        // Центральный пересчёт derived полей, чтобы create/update вели себя одинаково:
        // - eligibilityService решает eligible/compensation и какие документы нужны
        // - documentsService даёт какие документы реально загружены
        // - workflowService решает, как обновить статус в pre-submit зоне
        EligibilityResult eligibilityResult = eligibilityService.evaluate(
                modelMapper.map(claim.getIssue(), IssueDto.class),
                modelMapper.map(claim.getFlight(), FlightDto.class),
                modelMapper.map(claim.getEuContext(), EuContextDto.class),
                Optional.ofNullable(claim.getDocuments()).orElse(List.of())
                        .stream()
                        .map(d -> modelMapper.map(d, BoardingDocumentDto.class))
                        .collect(Collectors.toList())
        );

        claim.setEligible(eligibilityResult.getEligible());
        claim.setCompensationAmount(eligibilityResult.getCompensationAmount());

        Set<DocumentTypes> required = new HashSet<>(eligibilityResult.getRequiredDocuments());
        Set<DocumentTypes> uploaded = documentsService.uploadedTypes(claim);

        boolean hasAllRequired = uploaded.containsAll(required);
        claim.setStatus(workflowService.autoPreSubmitStatus(claim.getStatus(), hasAllRequired));
    }
}
