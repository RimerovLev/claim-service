package com.claims.mvp.claim.service.workflow;

import com.claims.mvp.claim.enums.ClaimStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
/**
 * ClaimWorkflowService.
 *
 * Отвечает за workflow статусов claim:
 * - какие переходы разрешены (FSM)
 * - автопереход между DOCS_REQUESTED и READY_TO_SUBMIT на этапе "до отправки"
 *
 * Важно: после SUBMITTED статус больше не трогаем автоматически — только ручные переходы.
 */
public class ClaimWorkflowServiceImpl implements ClaimWorkflowService {

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
    public void assertTransitionAllowed(ClaimStatus from, ClaimStatus to) {
        // Используется в ручной смене статуса (endpoint /status).
        if (from == null || to == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalArgumentException("Transition from " + from + " to " + to + " is not allowed");
        }
    }

    @Override
    public ClaimStatus autoPreSubmitStatus(ClaimStatus current, boolean hasAllRequiredDocuments) {
        // Авто-статус только в pre-submit зоне:
        // - если все required документы загружены -> READY_TO_SUBMIT
        // - иначе -> DOCS_REQUESTED
        // После SUBMITTED (и дальше) не вмешиваемся.
        if (current == ClaimStatus.NEW
                || current == ClaimStatus.DOCS_REQUESTED
                || current == ClaimStatus.READY_TO_SUBMIT) {
            return hasAllRequiredDocuments ? ClaimStatus.READY_TO_SUBMIT : ClaimStatus.DOCS_REQUESTED;
        }
        return current;
    }
}
