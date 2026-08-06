package com.hsbc.tms.rules.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.rules.dto.RuleAuditHistoryResponse;
import com.hsbc.tms.rules.dto.RuleCreateRequest;
import com.hsbc.tms.rules.dto.RuleResponse;
import com.hsbc.tms.rules.dto.RuleStatsResponse;
import com.hsbc.tms.rules.dto.RuleStatusUpdateRequest;
import com.hsbc.tms.rules.dto.RuleUpdateRequest;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleAuditAction;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.service.RuleService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleControllerTest {

    @Mock
    private RuleService ruleService;

    private RuleController controller;

    @BeforeEach
    void setUp() {
        controller = new RuleController(ruleService);
    }

    @Test
    void list_delegatesToService() {
        RuleResponse rule = sampleRule();
        when(ruleService.getRules(true, RuleType.VELOCITY, AlertSeverity.HIGH)).thenReturn(List.of(rule));

        List<RuleResponse> result = controller.list(true, RuleType.VELOCITY, AlertSeverity.HIGH);

        assertThat(result).containsExactly(rule);
        verify(ruleService).getRules(true, RuleType.VELOCITY, AlertSeverity.HIGH);
    }

    @Test
    void create_delegatesToService() {
        RuleCreateRequest request = new RuleCreateRequest(
                "Rule",
                RuleType.NEW_PAYEE,
                AlertSeverity.MEDIUM,
                true,
                null,
                null,
                null);
        RuleResponse response = sampleRule();
        when(ruleService.createRule(request)).thenReturn(response);

        RuleResponse result = controller.create(request);

        assertThat(result).isEqualTo(response);
        verify(ruleService).createRule(request);
    }

    @Test
    void update_delegatesToService() {
        RuleUpdateRequest request = new RuleUpdateRequest(
                "Rule Updated",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                true,
                new BigDecimal("1000.00"),
                null,
                null);
        RuleResponse response = sampleRule();
        when(ruleService.updateRule(5L, request)).thenReturn(response);

        RuleResponse result = controller.update(5L, request);

        assertThat(result).isEqualTo(response);
        verify(ruleService).updateRule(5L, request);
    }

    @Test
    void updateStatus_delegatesToService() {
        RuleStatusUpdateRequest request = new RuleStatusUpdateRequest(false);
        RuleResponse response = sampleRule();
        when(ruleService.updateRuleStatus(3L, request)).thenReturn(response);

        RuleResponse result = controller.updateStatus(3L, request);

        assertThat(result).isEqualTo(response);
        verify(ruleService).updateRuleStatus(3L, request);
    }

    @Test
    void delete_delegatesToService() {
        RuleResponse response = sampleRule();
        when(ruleService.softDeleteRule(8L)).thenReturn(response);

        RuleResponse result = controller.delete(8L);

        assertThat(result).isEqualTo(response);
        verify(ruleService).softDeleteRule(8L);
    }

    @Test
    void history_delegatesToService() {
        RuleAuditHistoryResponse item = new RuleAuditHistoryResponse(
                1L,
                9L,
                RuleAuditAction.CREATED,
                null,
                "new",
                Instant.parse("2026-08-05T10:00:00Z"),
                "SYSTEM");
        when(ruleService.getRuleHistory(9L)).thenReturn(List.of(item));

        List<RuleAuditHistoryResponse> result = controller.history(9L);

        assertThat(result).containsExactly(item);
        verify(ruleService).getRuleHistory(9L);
    }

    @Test
    void stats_delegatesToService() {
        RuleStatsResponse stats = new RuleStatsResponse(
                4,
                3,
                1,
                Map.of(RuleType.DAILY_LIMIT, 1L),
                Map.of(AlertSeverity.HIGH, 2L));
        when(ruleService.getRuleStats()).thenReturn(stats);

        RuleStatsResponse result = controller.stats();

        assertThat(result).isEqualTo(stats);
        verify(ruleService).getRuleStats();
    }

    private RuleResponse sampleRule() {
        return new RuleResponse(
                1L,
                "Rule",
                RuleType.NEW_PAYEE,
                AlertSeverity.MEDIUM,
                true,
                null,
                null,
                null);
    }
}

