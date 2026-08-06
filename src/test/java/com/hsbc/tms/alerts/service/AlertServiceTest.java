package com.hsbc.tms.alerts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import com.hsbc.tms.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(alertRepository, alertHistoryRepository, transactionRepository);
    }

    @Test
    void getAlerts_returnsMappedRows() {
        UUID transactionId = UUID.randomUUID();
        Alert alert = buildAlert(1L, AlertStatus.OPEN);
        alert.setTriggeringTransactionIds(List.of(transactionId));

        AlertHistory history = buildHistory(10L, 1L, null, AlertStatus.OPEN, "created", "system");
        Transaction transaction = buildTransaction(transactionId);

        when(alertRepository.search(eq(AlertStatus.OPEN), eq(AlertSeverity.HIGH), eq(true), org.mockito.ArgumentMatchers.<Set<AlertStatus>>any()))
                .thenReturn(List.of(alert));
        when(alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(history));
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        List<AlertResponse> rows = service.getAlerts(AlertStatus.OPEN, AlertSeverity.HIGH, true);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).id()).isEqualTo(1L);
        assertThat(rows.get(0).status()).isEqualTo(AlertStatus.OPEN);
        assertThat(rows.get(0).triggeringTransactions()).hasSize(1);
        assertThat(rows.get(0).history()).hasSize(1);
    }

    @Test
    void createAlert_savesAlertTransactionsAndHistory() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();

        CreateAlertRequest request = new CreateAlertRequest(
                "  High Amount Rule  ",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                "  Exceeded threshold  ",
                "  analyst-1  ",
                "  manual create  ",
                List.of(t1, t2, t1));

        when(transactionRepository.findById(t1)).thenReturn(Optional.of(buildTransaction(t1)));
        when(transactionRepository.findById(t2)).thenReturn(Optional.of(buildTransaction(t2)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(7L);
            return alert;
        });
        when(alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(buildHistory(1L, 7L, null, AlertStatus.OPEN, "manual create", "analyst-1")));

        AlertResponse created = service.createAlert(request);

        assertThat(created.id()).isEqualTo(7L);
        assertThat(created.ruleName()).isEqualTo("High Amount Rule");
        assertThat(created.message()).isEqualTo("Exceeded threshold");

        verify(alertRepository)
                .replaceTriggeringTransactions(eq(7L), org.mockito.ArgumentMatchers.argThat(ids -> ids.equals(List.of(t1, t2))));

        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isNull();
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo("analyst-1");
        assertThat(historyCaptor.getValue().getNote()).isEqualTo("manual create");
    }

    @Test
    void createAlert_throwsWhenTriggeringTransactionIsMissing() {
        UUID missingTransactionId = UUID.randomUUID();

        CreateAlertRequest request = new CreateAlertRequest(
                "Rule",
                RuleType.NEW_PAYEE,
                AlertSeverity.MEDIUM,
                "msg",
                "analyst",
                null,
                List.of(missingTransactionId));

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(22L);
            return alert;
        });
        when(transactionRepository.findById(missingTransactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAlert(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Triggering transaction not found: " + missingTransactionId);

        verify(alertRepository, never()).replaceTriggeringTransactions(eq(22L), org.mockito.ArgumentMatchers.<List<UUID>>any());
        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    void getAlert_throwsWhenMissing() {
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAlert(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Alert not found: 404");
    }

    @Test
    void getHistory_throwsWhenAlertMissing() {
        when(alertRepository.existsById(405L)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(405L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Alert not found: 405");
    }

    @Test
    void getHistory_returnsMappedHistory() {
        AlertHistory created = buildHistory(9L, 5L, null, AlertStatus.OPEN, "created", "system");
        AlertHistory changed = buildHistory(10L, 5L, AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, "investigating", "analyst");

        when(alertRepository.existsById(5L)).thenReturn(true);
        when(alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(created, changed));

        assertThat(service.getHistory(5L)).hasSize(2);
        assertThat(service.getHistory(5L).get(1).toStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    void updateStatus_throwsWhenAlertMissing() {
        when(alertRepository.findById(6L)).thenReturn(Optional.empty());

        AlertStatusUpdateRequest request = new AlertStatusUpdateRequest(AlertStatus.CLOSED, "analyst", "note");

        assertThatThrownBy(() -> service.updateStatus(6L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Alert not found: 6");
    }

    @Test
    void updateStatus_throwsWhenStatusUnchanged() {
        Alert alert = buildAlert(6L, AlertStatus.OPEN);
        when(alertRepository.findById(6L)).thenReturn(Optional.of(alert));

        AlertStatusUpdateRequest request = new AlertStatusUpdateRequest(AlertStatus.OPEN, "analyst", "note");

        assertThatThrownBy(() -> service.updateStatus(6L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Alert is already in status OPEN");
    }

    @Test
    void updateStatus_throwsForInvalidTransition() {
        Alert alert = buildAlert(6L, AlertStatus.CLOSED);
        when(alertRepository.findById(6L)).thenReturn(Optional.of(alert));

        AlertStatusUpdateRequest request = new AlertStatusUpdateRequest(AlertStatus.OPEN, "analyst", "note");

        assertThatThrownBy(() -> service.updateStatus(6L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid transition from CLOSED to OPEN");
    }

    @Test
    void updateStatus_transitionsAndAudits() {
        Alert alert = buildAlert(15L, AlertStatus.OPEN);

        when(alertRepository.findById(15L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(15L))
                .thenReturn(List.of(buildHistory(1L, 15L, AlertStatus.OPEN, AlertStatus.CLOSED, "closed", "analyst-1")));

        AlertResponse updated = service.updateStatus(
                15L,
                new AlertStatusUpdateRequest(AlertStatus.CLOSED, "  analyst-1  ", "  closed  "));

        assertThat(updated.status()).isEqualTo(AlertStatus.CLOSED);

        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo("analyst-1");
        assertThat(historyCaptor.getValue().getNote()).isEqualTo("closed");
    }

    @Test
    void createAlertForRuleTrigger_savesAlertAndHistoryWithUniqueTriggeringIds() {
        UUID sourceId = UUID.randomUUID();
        UUID anotherId = UUID.randomUUID();

        MonitoringRule rule = new MonitoringRule();
        rule.setName("Velocity Rule");
        rule.setType(RuleType.VELOCITY);
        rule.setSeverity(AlertSeverity.MEDIUM);

        Transaction source = buildTransaction(sourceId);
        Transaction duplicate = buildTransaction(sourceId);
        Transaction second = buildTransaction(anotherId);

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(50L);
            return alert;
        });

        service.createAlertForRuleTrigger(rule, source, "Velocity exceeded", List.of(duplicate, second));

        verify(alertRepository).replaceTriggeringTransactions(
                eq(50L),
                org.mockito.ArgumentMatchers.argThat(ids -> ids.equals(List.of(sourceId, anotherId))));

        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void resolveAlertsForTransactionDecision_closesAlertsForApprovedDecision() {
        UUID transactionId = UUID.randomUUID();
        Alert first = buildAlert(91L, AlertStatus.OPEN);
        Alert second = buildAlert(92L, AlertStatus.ACKNOWLEDGED);

        when(alertRepository.findActiveByTriggeringTransactionId(eq(transactionId), org.mockito.ArgumentMatchers.<Set<AlertStatus>>any()))
                .thenReturn(List.of(first, second));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveAlertsForTransactionDecision(transactionId, " analyst-2 ", true, null);

        assertThat(first.getStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(second.getStatus()).isEqualTo(AlertStatus.CLOSED);

        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository, org.mockito.Mockito.times(2)).save(historyCaptor.capture());
        assertThat(historyCaptor.getAllValues()).allMatch(h -> h.getToStatus() == AlertStatus.CLOSED);
        assertThat(historyCaptor.getAllValues()).allMatch(h -> h.getChangedBy().equals("analyst-2"));
    }

    @Test
    void resolveAlertsForTransactionDecision_dismissesAlertsForRejectedDecisionAndDefaultsActor() {
        UUID transactionId = UUID.randomUUID();
        Alert alert = buildAlert(93L, AlertStatus.INVESTIGATING);

        when(alertRepository.findActiveByTriggeringTransactionId(eq(transactionId), org.mockito.ArgumentMatchers.<Set<AlertStatus>>any()))
                .thenReturn(List.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveAlertsForTransactionDecision(transactionId, "   ", false, "  rejected by review  ");

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.DISMISSED);

        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(AlertStatus.DISMISSED);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo("system");
        assertThat(historyCaptor.getValue().getNote()).isEqualTo("rejected by review");
    }

    private Alert buildAlert(Long id, AlertStatus status) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setRuleName("Rule " + id);
        alert.setRuleType(RuleType.NEW_PAYEE);
        alert.setSeverity(AlertSeverity.MEDIUM);
        alert.setStatus(status);
        alert.setMessage("message");
        alert.setCreatedAt(Instant.parse("2026-08-06T08:00:00Z"));
        alert.setUpdatedAt(Instant.parse("2026-08-06T08:00:00Z"));
        alert.setTriggeringTransactionIds(List.of());
        return alert;
    }

    private AlertHistory buildHistory(
            Long id,
            Long alertId,
            AlertStatus from,
            AlertStatus to,
            String note,
            String changedBy) {
        AlertHistory history = new AlertHistory();
        history.setId(id);
        history.setAlertId(alertId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNote(note);
        history.setChangedBy(changedBy);
        history.setCreatedAt(Instant.parse("2026-08-06T08:00:00Z"));
        return history;
    }

    private Transaction buildTransaction(UUID id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAccountId("ACC-1");
        transaction.setPayeeId("PAY-1");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency("USD");
        transaction.setType(TransactionType.DEBIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionTime(Instant.parse("2026-08-06T07:55:00Z"));
        transaction.setCreatedAt(Instant.parse("2026-08-06T08:00:00Z"));
        transaction.setUpdatedAt(Instant.parse("2026-08-06T08:00:00Z"));
        return transaction;
    }
}

