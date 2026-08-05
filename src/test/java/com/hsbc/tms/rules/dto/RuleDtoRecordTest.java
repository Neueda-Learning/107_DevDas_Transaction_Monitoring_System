package com.hsbc.tms.rules.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleAuditAction;
import com.hsbc.tms.rules.model.RuleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleDtoRecordTest {

    @Test
    void ruleCreateRequest_exposesAllValues() {
        RuleCreateRequest request = new RuleCreateRequest(
                "High Amount",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                true,
                new BigDecimal("10000.00"),
                5,
                10);

        assertThat(request.name()).isEqualTo("High Amount");
        assertThat(request.type()).isEqualTo(RuleType.AMOUNT_THRESHOLD);
        assertThat(request.severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(request.active()).isTrue();
        assertThat(request.amountThreshold()).isEqualByComparingTo("10000.00");
        assertThat(request.transactionCountThreshold()).isEqualTo(5);
        assertThat(request.timeWindowMinutes()).isEqualTo(10);
    }

    @Test
    void ruleUpdateRequest_exposesAllValues() {
        RuleUpdateRequest request = new RuleUpdateRequest(
                "Velocity",
                RuleType.VELOCITY,
                AlertSeverity.MEDIUM,
                false,
                null,
                7,
                15);

        assertThat(request.name()).isEqualTo("Velocity");
        assertThat(request.type()).isEqualTo(RuleType.VELOCITY);
        assertThat(request.severity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(request.active()).isFalse();
        assertThat(request.amountThreshold()).isNull();
        assertThat(request.transactionCountThreshold()).isEqualTo(7);
        assertThat(request.timeWindowMinutes()).isEqualTo(15);
    }

    @Test
    void ruleStatusUpdateRequest_exposesValue() {
        RuleStatusUpdateRequest request = new RuleStatusUpdateRequest(true);
        assertThat(request.active()).isTrue();
    }

    @Test
    void ruleResponse_exposesAllValues() {
        RuleResponse response = new RuleResponse(
                1L,
                "New Payee",
                RuleType.NEW_PAYEE,
                AlertSeverity.LOW,
                true,
                null,
                null,
                null);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("New Payee");
        assertThat(response.type()).isEqualTo(RuleType.NEW_PAYEE);
        assertThat(response.severity()).isEqualTo(AlertSeverity.LOW);
        assertThat(response.active()).isTrue();
    }

    @Test
    void ruleAuditHistoryResponse_exposesAllValues() {
        Instant changedAt = Instant.parse("2026-08-05T10:00:00Z");
        RuleAuditHistoryResponse response = new RuleAuditHistoryResponse(
                10L,
                7L,
                RuleAuditAction.CREATED,
                null,
                "new-values",
                changedAt,
                "SYSTEM");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.ruleId()).isEqualTo(7L);
        assertThat(response.action()).isEqualTo(RuleAuditAction.CREATED);
        assertThat(response.previousValues()).isNull();
        assertThat(response.newValues()).isEqualTo("new-values");
        assertThat(response.changedAt()).isEqualTo(changedAt);
        assertThat(response.changedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void ruleStatsResponse_exposesAllValues() {
        Map<RuleType, Long> byType = Map.of(RuleType.DAILY_LIMIT, 1L);
        Map<AlertSeverity, Long> bySeverity = Map.of(AlertSeverity.HIGH, 2L);

        RuleStatsResponse response = new RuleStatsResponse(4, 3, 1, byType, bySeverity);

        assertThat(response.totalRules()).isEqualTo(4);
        assertThat(response.activeRules()).isEqualTo(3);
        assertThat(response.inactiveRules()).isEqualTo(1);
        assertThat(response.rulesByType()).containsEntry(RuleType.DAILY_LIMIT, 1L);
        assertThat(response.rulesBySeverity()).containsEntry(AlertSeverity.HIGH, 2L);
    }
}

