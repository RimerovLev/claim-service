package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dao.ClaimRepository;
import com.claims.mvp.claim.enums.DocumentTypes;
import com.claims.mvp.claim.enums.EventTypes;
import com.claims.mvp.eligibility.dto.EligibilityResult;
import com.claims.mvp.eligibility.service.EligibilityService;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.events.dto.EventsResponseDto;
import com.claims.mvp.events.model.ClaimEvents;
import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.claim.dto.*;
import com.claims.mvp.claim.enums.ClaimStatus;
import com.claims.mvp.claim.model.*;
import com.claims.mvp.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ClaimServiceImpl implements ClaimService {
    final ClaimRepository claimRepository;
    final UserRepository userRepository;
    final ModelMapper modelMapper;
    final EligibilityService eligibilityService;
    final EventsRepository eventsRepository;

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED_TRANSITIONS = Map.of(
            ClaimStatus.NEW, Set.of(ClaimStatus.DOCS_REQUESTED, ClaimStatus.READY_TO_SUBMIT),
            ClaimStatus.DOCS_REQUESTED, Set.of(ClaimStatus.READY_TO_SUBMIT),
            ClaimStatus.READY_TO_SUBMIT, Set.of(ClaimStatus.SUBMITTED),
            ClaimStatus.SUBMITTED, Set.of(ClaimStatus.FOLLOW_UP_SENT, ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.FOLLOW_UP_SENT, Set.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.APPROVED, Set.of(ClaimStatus.PAID),
            ClaimStatus.REJECTED, Set.of(ClaimStatus.CLOSED),
            ClaimStatus.PAID, Set.of(ClaimStatus.CLOSED)
    );

    @Override
    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request) {
        Claim claim = new Claim();
        User user = userRepository.findById(request.getUserId()).orElseThrow(
                () -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
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

        claim.setDocuments(mapDocuments(request.getDocuments(), claim));

        // Один общий пересчёт, чтобы логика была в одном месте и create/update вели себя одинаково.
        syncEligibilityAndDocsStatus(claim);

        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    @Transactional
    public ClaimResponse updateClaimDetails(Long id, UpdateClaimDetails request) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));

        if(request.getFlight() != null) {
            if (claim.getFlight() == null){
                Flight flight = new Flight();
                flight.setClaim(claim);
                claim.setFlight(flight);
            }
            modelMapper.map(request.getFlight(), claim.getFlight());
        }
        if(request.getIssue() != null) {
            if (claim.getIssue() == null){
                Issue issue = new Issue();
                issue.setClaim(claim);
                claim.setIssue(issue);
            }
            modelMapper.map(request.getIssue(), claim.getIssue());
        }
        if(request.getEuContext() != null) {
            if (claim.getEuContext() == null){
                EuContext euContext = new EuContext();
                euContext.setClaim(claim);
                claim.setEuContext(euContext);
            }
            modelMapper.map(request.getEuContext(), claim.getEuContext());
        }
        if (request.getDocuments() != null) {
            mergeDocuments(claim, request.getDocuments());
        }
        if (claim.getFlight() == null || claim.getIssue() == null || claim.getEuContext() == null){
            throw new IllegalArgumentException("Claim details incomplete");
        }
        // После любых изменений деталей пересчитываем derived поля, чтобы claim был консистентным.
        syncEligibilityAndDocsStatus(claim);

        claimRepository.save(claim);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Transactional
    public ClaimResponse updateClaimStatus(Long id, StatusChangeRequest request) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Claim not found with id: " + id));
        if(!isTransitionAllowed(claim.getStatus(), request.getStatus())) {
            throw new IllegalArgumentException("Transition from " + claim.getStatus() + " to " + request.getStatus() + " is not allowed");
        }
        ClaimEvents claimEvents = new ClaimEvents();
        claimEvents.setClaim(claim);
        claimEvents.setType(EventTypes.STATUS_CHANGED);
        String safeNote = Optional.ofNullable(request.getNote()).orElse("");
        String payload = "{\"from\":\"" + claim.getStatus() + "\",\"to\":\"" + request.getStatus() + "\",\"note\":\"" + safeNote + "\"}";
        claimEvents.setPayload(payload);

        claim.setStatus(request.getStatus());
        claimRepository.save(claim);

        eventsRepository.save(claimEvents);
        return modelMapper.map(claim, ClaimResponse.class);
    }

    @Override
    public List<EventsResponseDto> getClaimEvents(Long id) {
        if (!claimRepository.existsById(id)) {
            throw new EntityNotFoundException("Claim not found with id: " + id);
        }
        return eventsRepository.findByClaimIdOrderByCreatedAtDesc(id).stream()
                .map(event -> modelMapper.map(event, EventsResponseDto.class))
                .collect(Collectors.toList());
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

    private boolean isTransitionAllowed(ClaimStatus from, ClaimStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    private void syncEligibilityAndDocsStatus(Claim claim) {
        // Централизованный пересчёт "derived" полей:
        // - eligible / compensationAmount рассчитываются EligibilityService
        // - статус автоматически поднимаем только в пределах "pre-submit" зоны (NEW/DOCS_REQUESTED/READY_TO_SUBMIT),
        //   чтобы не ломать ручные статусы типа SUBMITTED/APPROVED и т.п.
        EligibilityResult eligibilityResult = eligibilityService.evaluate(
                modelMapper.map(claim.getIssue(), IssueDto.class),
                modelMapper.map(claim.getFlight(), FlightDto.class),
                modelMapper.map(claim.getEuContext(), EuContextDto.class),
                Optional.ofNullable(claim.getDocuments()).orElse(List.of())
                        .stream()
                        .map(document -> modelMapper.map(document, BoardingDocumentDto.class))
                        .collect(Collectors.toList()));

        claim.setEligible(eligibilityResult.getEligible());
        claim.setCompensationAmount(eligibilityResult.getCompensationAmount());

        Set<DocumentTypes> requiredDocuments = new HashSet<>(eligibilityResult.getRequiredDocuments());
        Set<DocumentTypes> uploadedDocuments = Optional.ofNullable(claim.getDocuments()).orElse(List.of())
                .stream()
                .map(BoardingDocuments::getType)
                .collect(Collectors.toSet());

        // Правило: uploaded содержит все required (лишние документы допускаются).
        boolean hasAllRequiredDocuments = uploadedDocuments.containsAll(requiredDocuments);

        if(claim.getStatus() == ClaimStatus.NEW
        || claim.getStatus() == ClaimStatus.DOCS_REQUESTED
        || claim.getStatus() == ClaimStatus.READY_TO_SUBMIT){
            claim.setStatus(hasAllRequiredDocuments ? ClaimStatus.READY_TO_SUBMIT : ClaimStatus.DOCS_REQUESTED);
        }
    }

    private List<BoardingDocuments> mapDocuments(List<BoardingDocumentDto> documentDtos, Claim claim) {
        // Create-case: на создании claim мы не "мержим" документы, а строим новый список сущностей.
        // - если documentDtos == null → считаем, что документов пока нет (пустой список)
        // - каждый DTO превращаем в BoardingDocuments и обязательно проставляем связь document.claim = claim
        // - target=null означает "создать новый BoardingDocuments" (см. toBoardingDocument)
        return Optional.ofNullable(documentDtos)
                .orElse(List.of())
                .stream()
                .map(documentDto -> toBoardingDocument(documentDto, claim, null))
                .collect(Collectors.toList());
    }

    private void mergeDocuments(Claim claim, List<BoardingDocumentDto> documentDtos) {
        if (claim.getDocuments() == null) {
            claim.setDocuments(new ArrayList<>());
        }

        // Update-case: документы можно "дозагружать" частями.
        // Мержим по типу документа (DocumentTypes):
        // - если такой тип уже есть -> обновляем существующую строку
        // - если типа не было -> добавляем новую строку
        Map<DocumentTypes, BoardingDocuments> documentsByType = claim.getDocuments()
                .stream()
                .collect(Collectors.toMap(
                        BoardingDocuments::getType,
                        document -> document,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        for (BoardingDocumentDto documentDto : documentDtos) {
            BoardingDocuments existingDocument = documentsByType.get(documentDto.getType());
            BoardingDocuments document = toBoardingDocument(documentDto, claim, existingDocument);
            documentsByType.put(document.getType(), document);
        }

        // Важно для JPA orphanRemoval=true:
        // нельзя делать claim.setDocuments(newList) — Hibernate может решить, что старые элементы "осиротели"
        // и попытается удалить их, а затем заново вставить. Поэтому мутируем managed коллекцию in-place.
        claim.getDocuments().clear();
        claim.getDocuments().addAll(documentsByType.values());
    }

    private BoardingDocuments toBoardingDocument(BoardingDocumentDto documentDto, Claim claim, BoardingDocuments target) {
        // Если target == null, это новый документ; иначе обновляем уже существующий entity.
        BoardingDocuments document = target == null ? new BoardingDocuments() : target;
        // documents.id у нас String: генерим UUID, если клиент не прислал id.
        document.setId(documentDto.getId() != null ? documentDto.getId() : Optional.ofNullable(document.getId()).orElse(UUID.randomUUID().toString()));
        document.setClaim(claim);
        document.setType(documentDto.getType());
        document.setUrl(documentDto.getUrl());
        return document;
    }

}
