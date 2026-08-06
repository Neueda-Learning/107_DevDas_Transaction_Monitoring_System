package com.hsbc.tms.rules.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleAuditAction;
import com.hsbc.tms.rules.model.RuleExecutionOutcome;
import com.hsbc.tms.rules.model.RuleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RuleEntitiesTest {

    @Test
    void monitoringRule_gettersAndSettersWork() {
        Instant createdAt = Instant.parse("2026-08-05T00:00:00Z");

        MonitoringRule rule = new MonitoringRule();
        rule.setId(1L);
        rule.setName("Rule");
        rule.setType(RuleType.VELOCITY);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setActive(true);
        rule.setAmountThreshold(new BigDecimal("100.00"));
        rule.setTransactionCountThreshold(5);
        rule.setTimeWindowMinutes(10);
        rule.setCreatedAt(createdAt);

        assertThat(rule.getId()).isEqualTo(1L);
        assertThat(rule.getName()).isEqualTo("Rule");
        assertThat(rule.getType()).isEqualTo(RuleType.VELOCITY);
        assertThat(rule.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(rule.isActive()).isTrue();
        assertThat(rule.getAmountThreshold()).isEqualByComparingTo("100.00");
        assertThat(rule.getTransactionCountThreshold()).isEqualTo(5);
        assertThat(rule.getTimeWindowMinutes()).isEqualTo(10);
        assertThat(rule.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void ruleAuditHistory_gettersAndSettersWork() {
        Instant changedAt = Instant.parse("2026-08-05T10:00:00Z");

        RuleAuditHistory history = new RuleAuditHistory();
        history.setId(2L);
        history.setRuleId(1L);
        history.setAction(RuleAuditAction.UPDATED);
        history.setPreviousValues("old");
        history.setNewValues("new");
        history.setChangedAt(changedAt);
        history.setChangedBy("SYSTEM");

        assertThat(history.getId()).isEqualTo(2L);
        assertThat(history.getRuleId()).isEqualTo(1L);
        assertThat(history.getAction()).isEqualTo(RuleAuditAction.UPDATED);
        assertThat(history.getPreviousValues()).isEqualTo("old");
        assertThat(history.getNewValues()).isEqualTo("new");
        assertThat(history.getChangedAt()).isEqualTo(changedAt);
        assertThat(history.getChangedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void ruleExecutionHistory_gettersAndSettersWork() {
        UUID executionId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-05T12:00:00Z");

        RuleExecutionHistory history = new RuleExecutionHistory();
        history.setId(3L);
        history.setExecutionId(executionId);
        history.setRuleId(10L);
        history.setTransactionId(transactionId);
        history.setOutcome(RuleExecutionOutcome.TRIGGERED);
        history.setMessage("Triggered");
        history.setCreatedAt(createdAt);

        assertThat(history.getId()).isEqualTo(3L);
        assertThat(history.getExecutionId()).isEqualTo(executionId);
        assertThat(history.getRuleId()).isEqualTo(10L);
        assertThat(history.getTransactionId()).isEqualTo(transactionId);
        assertThat(history.getOutcome()).isEqualTo(RuleExecutionOutcome.TRIGGERED);
        assertThat(history.getMessage()).isEqualTo("Triggered");
        assertThat(history.getCreatedAt()).isEqualTo(createdAt);
    }
}

