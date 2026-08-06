package com.hsbc.tms.rules.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.common.exception.ConflictException;
import com.hsbc.tms.common.exception.ResourceNotFoundException;
import com.hsbc.tms.rules.dto.RuleCreateRequest;
import com.hsbc.tms.rules.dto.RuleResponse;
import com.hsbc.tms.rules.dto.RuleStatsResponse;
import com.hsbc.tms.rules.dto.RuleStatusUpdateRequest;
import com.hsbc.tms.rules.dto.RuleUpdateRequest;
import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.entity.RuleAuditHistory;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleAuditAction;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.MonitoringRuleRepository;
import com.hsbc.tms.rules.repository.RuleAuditHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private MonitoringRuleRepository ruleRepository;

    @Mock
    private RuleAuditHistoryRepository ruleAuditHistoryRepository;

    @Mock
    private RuleValidationService ruleValidationService;

    private RuleService service;

    @BeforeEach
    void setUp() {
        service = new RuleService(ruleRepository, ruleAuditHistoryRepository, ruleValidationService);
    }

    @Test
    void getRules_returnsMappedRows() {
        MonitoringRule rule = buildRule(1L, "High Amount", RuleType.AMOUNT_THRESHOLD, AlertSeverity.HIGH, true);
        when(ruleRepository.search(true, RuleType.AMOUNT_THRESHOLD, AlertSeverity.HIGH)).thenReturn(List.of(rule));

        List<RuleResponse> responses = service.getRules(true, RuleType.AMOUNT_THRESHOLD, AlertSeverity.HIGH);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("High Amount");
    }

    @Test
    void getRuleStats_returnsAggregatedCounts() {
        when(ruleRepository.count()).thenReturn(4L);
        when(ruleRepository.countByActiveTrue()).thenReturn(3L);
        when(ruleRepository.countByActiveFalse()).thenReturn(1L);
        when(ruleRepository.countGroupedByType()).thenReturn(Map.of(RuleType.VELOCITY, 2L));
        when(ruleRepository.countGroupedBySeverity()).thenReturn(Map.of(AlertSeverity.HIGH, 1L));

        RuleStatsResponse stats = service.getRuleStats();

        assertThat(stats.totalRules()).isEqualTo(4);
        assertThat(stats.activeRules()).isEqualTo(3);
        assertThat(stats.inactiveRules()).isEqualTo(1);
        assertThat(stats.rulesByType()).containsEntry(RuleType.VELOCITY, 2L);
        assertThat(stats.rulesBySeverity()).containsEntry(AlertSeverity.HIGH, 1L);
    }

    @Test
    void getRuleHistory_throwsWhenRuleDoesNotExist() {
        when(ruleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getRuleHistory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rule with id 99 not found");
    }

    @Test
    void getRuleHistory_returnsMappedHistory() {
        RuleAuditHistory row = new RuleAuditHistory();
        row.setId(2L);
        row.setRuleId(7L);
        row.setAction(RuleAuditAction.CREATED);
        row.setChangedBy("SYSTEM");
        row.setChangedAt(Instant.parse("2026-08-05T10:15:30Z"));

        when(ruleRepository.existsById(7L)).thenReturn(true);
        when(ruleAuditHistoryRepository.findByRuleIdOrderByChangedAtAsc(7L)).thenReturn(List.of(row));

        assertThat(service.getRuleHistory(7L)).hasSize(1);
        assertThat(service.getRuleHistory(7L).get(0).action()).isEqualTo(RuleAuditAction.CREATED);
    }

    @Test
    void createRule_savesRuleAndAudit() {
        RuleCreateRequest request = new RuleCreateRequest(
                "  High Amount  ",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                null,
                new BigDecimal("10000.00"),
                null,
                null);

        when(ruleRepository.existsByNameIgnoreCase("High Amount")).thenReturn(false);
        when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> {
            MonitoringRule toSave = invocation.getArgument(0);
            toSave.setId(10L);
            toSave.setCreatedAt(Instant.parse("2026-08-05T00:00:00Z"));
            return toSave;
        });

        RuleResponse response = service.createRule(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("High Amount");
        assertThat(response.active()).isTrue();
        assertThat(response.amountThreshold()).isEqualByComparingTo("10000.00");
        assertThat(response.transactionCountThreshold()).isNull();
        assertThat(response.timeWindowMinutes()).isNull();

        ArgumentCaptor<RuleAuditHistory> historyCaptor = ArgumentCaptor.forClass(RuleAuditHistory.class);
        verify(ruleAuditHistoryRepository).save(historyCaptor.capture());
        RuleAuditHistory audit = historyCaptor.getValue();
        assertThat(audit.getRuleId()).isEqualTo(10L);
        assertThat(audit.getAction()).isEqualTo(RuleAuditAction.CREATED);
        assertThat(audit.getPreviousValues()).isNull();
        assertThat(audit.getNewValues()).contains("name='High Amount'");
    }

    @Test
    void createRule_throwsWhenNameAlreadyExists() {
        RuleCreateRequest request = new RuleCreateRequest(
                "Rule",
                RuleType.NEW_PAYEE,
                AlertSeverity.MEDIUM,
                true,
                null,
                null,
                null);
        when(ruleRepository.existsByNameIgnoreCase("Rule")).thenReturn(true);

        assertThatThrownBy(() -> service.createRule(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Rule name already exists: Rule");

        verify(ruleRepository, never()).save(any(MonitoringRule.class));
    }

    @Test
    void createRule_keepsExplicitInactiveFlag() {
        RuleCreateRequest request = new RuleCreateRequest(
                "Inactive Rule",
                RuleType.NEW_PAYEE,
                AlertSeverity.LOW,
                false,
                null,
                null,
                null);

        when(ruleRepository.existsByNameIgnoreCase("Inactive Rule")).thenReturn(false);
        when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> {
            MonitoringRule toSave = invocation.getArgument(0);
            toSave.setId(25L);
            return toSave;
        });

        RuleResponse response = service.createRule(request);

        assertThat(response.id()).isEqualTo(25L);
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateRule_throwsWhenRuleNotFound() {
        when(ruleRepository.findById(33L)).thenReturn(Optional.empty());

        RuleUpdateRequest request = new RuleUpdateRequest(
                "Updated",
                RuleType.NEW_PAYEE,
                AlertSeverity.LOW,
                true,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.updateRule(33L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rule with id 33 not found");
    }

    @Test
    void updateRule_throwsWhenDuplicateNameExistsForAnotherRule() {
        MonitoringRule existing = buildRule(5L, "Old", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, true);
        when(ruleRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(ruleRepository.existsByNameIgnoreCaseAndIdNot("Duplicate", 5L)).thenReturn(true);

        RuleUpdateRequest request = new RuleUpdateRequest(
                "Duplicate",
                RuleType.NEW_PAYEE,
                AlertSeverity.MEDIUM,
                false,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.updateRule(5L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Rule name already exists: Duplicate");
    }

    @Test
    void updateRule_updatesAndAudits() {
        MonitoringRule existing = buildRule(8L, "Old Velocity", RuleType.VELOCITY, AlertSeverity.MEDIUM, true);
        existing.setTransactionCountThreshold(3);
        existing.setTimeWindowMinutes(5);

        when(ruleRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(ruleRepository.existsByNameIgnoreCaseAndIdNot("Velocity Updated", 8L)).thenReturn(false);
        when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleUpdateRequest request = new RuleUpdateRequest(
                "  Velocity Updated  ",
                RuleType.VELOCITY,
                AlertSeverity.HIGH,
                false,
                null,
                6,
                10);

        RuleResponse updated = service.updateRule(8L, request);

        assertThat(updated.name()).isEqualTo("Velocity Updated");
        assertThat(updated.severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(updated.active()).isFalse();
        assertThat(updated.transactionCountThreshold()).isEqualTo(6);
        assertThat(updated.timeWindowMinutes()).isEqualTo(10);

        ArgumentCaptor<RuleAuditHistory> historyCaptor = ArgumentCaptor.forClass(RuleAuditHistory.class);
        verify(ruleAuditHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(RuleAuditAction.UPDATED);
    }

    @Test
    void updateRuleStatus_throwsWhenStatusIsUnchanged() {
        MonitoringRule existing = buildRule(1L, "Rule", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, true);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateRuleStatus(1L, new RuleStatusUpdateRequest(true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rule with id 1 is already active");
    }

    @Test
    void updateRuleStatus_changesStatusAndAudits() {
        MonitoringRule existing = buildRule(2L, "Rule", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, false);
        when(ruleRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = service.updateRuleStatus(2L, new RuleStatusUpdateRequest(true));

        assertThat(response.active()).isTrue();

        ArgumentCaptor<RuleAuditHistory> historyCaptor = ArgumentCaptor.forClass(RuleAuditHistory.class);
        verify(ruleAuditHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(RuleAuditAction.ACTIVATED);
    }

    @Test
    void updateRuleStatus_deactivatesAndAudits() {
        MonitoringRule existing = buildRule(3L, "Rule", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, true);
        when(ruleRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = service.updateRuleStatus(3L, new RuleStatusUpdateRequest(false));

        assertThat(response.active()).isFalse();

        ArgumentCaptor<RuleAuditHistory> historyCaptor = ArgumentCaptor.forClass(RuleAuditHistory.class);
        verify(ruleAuditHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(RuleAuditAction.DEACTIVATED);
    }

    @Test
    void softDeleteRule_setsInactiveAndAuditsDelete() {
        MonitoringRule existing = buildRule(9L, "Rule", RuleType.DAILY_LIMIT, AlertSeverity.HIGH, true);
        when(ruleRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = service.softDeleteRule(9L);

        assertThat(response.active()).isFalse();

        ArgumentCaptor<RuleAuditHistory> historyCaptor = ArgumentCaptor.forClass(RuleAuditHistory.class);
        verify(ruleAuditHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(RuleAuditAction.DELETED);
    }

    private MonitoringRule buildRule(Long id, String name, RuleType type, AlertSeverity severity, boolean active) {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(id);
        rule.setName(name);
        rule.setType(type);
        rule.setSeverity(severity);
        rule.setActive(active);
        rule.setCreatedAt(Instant.parse("2026-08-05T00:00:00Z"));
        return rule;
    }
}

