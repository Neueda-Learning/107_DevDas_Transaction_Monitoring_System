package com.hsbc.tms.alerts.service;

import com.hsbc.tms.alerts.dto.AlertHistoryResponse;
import com.hsbc.tms.alerts.dto.AlertResponse;
import com.hsbc.tms.alerts.dto.AlertStatusUpdateRequest;
import com.hsbc.tms.alerts.dto.CreateAlertRequest;
import com.hsbc.tms.alerts.entity.Alert;
import com.hsbc.tms.alerts.entity.AlertHistory;
import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.alerts.repository.AlertHistoryRepository;
import com.hsbc.tms.alerts.repository.AlertRepository;
import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.common.exception.ResourceNotFoundException;
import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.repository.TransactionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private static final Set<AlertStatus> ACTIVE_STATUSES = Set.of(
            AlertStatus.OPEN,
            AlertStatus.ACKNOWLEDGED,
            AlertStatus.INVESTIGATING);

    private static final Map<AlertStatus, Set<AlertStatus>> VALID_TRANSITIONS = Map.of(
            AlertStatus.OPEN, Set.of(AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING, AlertStatus.CLOSED, AlertStatus.DISMISSED),
            AlertStatus.ACKNOWLEDGED, Set.of(AlertStatus.INVESTIGATING, AlertStatus.CLOSED, AlertStatus.DISMISSED),
            AlertStatus.INVESTIGATING, Set.of(AlertStatus.CLOSED, AlertStatus.DISMISSED),
            AlertStatus.CLOSED, Set.of(),
            AlertStatus.DISMISSED, Set.of());

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final TransactionRepository transactionRepository;

    public AlertService(
            AlertRepository alertRepository,
            AlertHistoryRepository alertHistoryRepository,
            TransactionRepository transactionRepository) {
        this.alertRepository = alertRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity, boolean activeOnly) {
        return alertRepository.search(status, severity, activeOnly, ACTIVE_STATUSES).stream()
                .map(this::toAlertResponse)
                .toList();
    }

    @Transactional
    public AlertResponse createAlert(CreateAlertRequest request) {
        Alert alert = new Alert();
        alert.setRuleName(request.ruleName().trim());
        alert.setRuleType(request.ruleType());
        alert.setSeverity(request.severity());
        alert.setStatus(AlertStatus.OPEN);
        alert.setMessage(request.message().trim());

        Instant now = Instant.now();
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);

        Alert saved = alertRepository.save(alert);

        List<UUID> triggeringIds = normalizeTriggeringTransactionIds(request.triggeringTransactionIds());
        validateTransactionsExist(triggeringIds);
        alertRepository.replaceTriggeringTransactions(saved.getId(), triggeringIds);
        saved.setTriggeringTransactionIds(triggeringIds);

        AlertHistory history = new AlertHistory();
        history.setAlertId(saved.getId());
        history.setFromStatus(null);
        history.setToStatus(AlertStatus.OPEN);
        history.setNote(normalizeNote(request.note(), "Alert created"));
        history.setChangedBy(request.operatorId().trim());
        history.setCreatedAt(now);
        alertHistoryRepository.save(history);

        return toAlertResponse(saved);
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
        return toAlertResponse(alert);
    }

    @Transactional(readOnly = true)
    public List<AlertHistoryResponse> getHistory(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alert not found: " + id);
        }

        return alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(id).stream()
                .map(AlertApiMapper::toAlertHistoryResponse)
                .toList();
    }

    @Transactional
    public AlertResponse updateStatus(Long id, AlertStatusUpdateRequest request) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));

        if (alert.getStatus() == request.status()) {
            throw new BadRequestException("Alert is already in status " + request.status());
        }

        Set<AlertStatus> allowed = VALID_TRANSITIONS.getOrDefault(alert.getStatus(), Set.of());
        if (!allowed.contains(request.status())) {
            throw new BadRequestException("Invalid transition from " + alert.getStatus() + " to " + request.status());
        }

        transitionAlert(alert, request.status(), request.operatorId().trim(), normalizeNote(request.note(), "Status updated"));
        syncLinkedTransactionsForAlertDecision(alert, request.status(), request.operatorId(), request.note());
        return toAlertResponse(alert);
    }

    @Transactional
    public void createAlertForRuleTrigger(
            MonitoringRule rule,
            Transaction transaction,
            String message,
            List<Transaction> triggeringTransactions) {
        Alert alert = new Alert();
        alert.setRuleName(rule.getName());
        alert.setRuleType(rule.getType());
        alert.setSeverity(rule.getSeverity());
        alert.setStatus(AlertStatus.OPEN);
        alert.setMessage(message);

        Instant now = Instant.now();
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);

        Alert saved = alertRepository.save(alert);

        LinkedHashSet<UUID> triggeringIds = new LinkedHashSet<>();
        triggeringIds.add(transaction.getId());
        if (triggeringTransactions != null) {
            for (Transaction triggeringTransaction : triggeringTransactions) {
                triggeringIds.add(triggeringTransaction.getId());
            }
        }

        alertRepository.replaceTriggeringTransactions(saved.getId(), new ArrayList<>(triggeringIds));

        AlertHistory history = new AlertHistory();
        history.setAlertId(saved.getId());
        history.setFromStatus(null);
        history.setToStatus(AlertStatus.OPEN);
        history.setNote("Alert created by rule evaluation");
        history.setChangedBy("SYSTEM");
        history.setCreatedAt(now);
        alertHistoryRepository.save(history);
    }

    @Transactional
    public void resolveAlertsForTransactionDecision(UUID transactionId, String operatorId, boolean approved, String note) {
        String actor = operatorId == null || operatorId.isBlank() ? "system" : operatorId.trim();
        AlertStatus nextStatus = approved ? AlertStatus.CLOSED : AlertStatus.DISMISSED;
        String defaultNote = approved
                ? "Transaction approved. Alert closed by decision workflow."
                : "Transaction rejected. Alert dismissed by decision workflow.";

        List<Alert> alerts = alertRepository.findActiveByTriggeringTransactionId(transactionId, ACTIVE_STATUSES);
        for (Alert alert : alerts) {
            transitionAlert(alert, nextStatus, actor, normalizeNote(note, defaultNote));
        }
    }

    private AlertResponse toAlertResponse(Alert alert) {
        List<AlertHistoryResponse> historyRows = alertHistoryRepository
                .findByAlertIdOrderByCreatedAtAsc(alert.getId())
                .stream()
                .map(AlertApiMapper::toAlertHistoryResponse)
                .toList();

        List<TransactionResponse> transactionRows = alert.getTriggeringTransactionIds().stream()
                .map(transactionRepository::findById)
                .flatMap(Optional::stream)
                .map(AlertApiMapper::toTransactionResponse)
                .toList();

        return AlertApiMapper.toAlertResponse(alert, transactionRows, historyRows);
    }

    private List<UUID> normalizeTriggeringTransactionIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private void validateTransactionsExist(List<UUID> transactionIds) {
        for (UUID transactionId : transactionIds) {
            if (transactionRepository.findById(transactionId).isEmpty()) {
                throw new BadRequestException("Triggering transaction not found: " + transactionId);
            }
        }
    }

    private void transitionAlert(Alert alert, AlertStatus nextStatus, String changedBy, String note) {
        AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(nextStatus);
        alert.setUpdatedAt(Instant.now());
        alertRepository.save(alert);

        AlertHistory history = new AlertHistory();
        history.setAlertId(alert.getId());
        history.setFromStatus(previousStatus);
        history.setToStatus(nextStatus);
        history.setNote(note);
        history.setChangedBy(changedBy);
        history.setCreatedAt(Instant.now());
        alertHistoryRepository.save(history);
    }

    private void syncLinkedTransactionsForAlertDecision(Alert alert, AlertStatus alertStatus, String operatorId, String note) {
        TransactionStatus nextTransactionStatus = switch (alertStatus) {
            case CLOSED -> TransactionStatus.COMPLETED;
            case DISMISSED -> TransactionStatus.REJECTED;
            default -> null;
        };

        if (nextTransactionStatus == null) {
            return;
        }

        String actor = operatorId == null || operatorId.isBlank() ? "system" : operatorId.trim();
        String defaultNote = switch (nextTransactionStatus) {
            case COMPLETED -> "Alert closed by alert workflow.";
            case REJECTED -> "Alert dismissed by alert workflow.";
            default -> "Status updated by alert workflow.";
        };
        String normalizedNote = normalizeNote(note, defaultNote);
        Instant now = Instant.now();

        for (UUID transactionId : alert.getTriggeringTransactionIds()) {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Triggering transaction not found: " + transactionId));

            if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
                continue;
            }

            transaction.setStatus(nextTransactionStatus);
            transaction.setReviewedBy(actor);
            transaction.setReviewedAt(now);
            transaction.setReviewNote(normalizedNote);
            transaction.setUpdatedAt(now);
            transactionRepository.update(transaction);
        }
    }

    private String normalizeNote(String note, String defaultNote) {
        if (note == null || note.isBlank()) {
            return defaultNote;
        }
        return note.trim();
    }
}




