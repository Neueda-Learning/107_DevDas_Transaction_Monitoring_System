package com.hsbc.tms.rules.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleModelsTest {

    @Test
    void ruleType_containsExpectedValues() {
        assertThat(RuleType.values())
                .containsExactly(RuleType.AMOUNT_THRESHOLD, RuleType.VELOCITY, RuleType.NEW_PAYEE, RuleType.DAILY_LIMIT);
        assertThat(RuleType.valueOf("VELOCITY")).isEqualTo(RuleType.VELOCITY);
    }

    @Test
    void alertSeverity_containsExpectedValues() {
        assertThat(AlertSeverity.values()).containsExactly(AlertSeverity.HIGH, AlertSeverity.MEDIUM, AlertSeverity.LOW);
        assertThat(AlertSeverity.valueOf("HIGH")).isEqualTo(AlertSeverity.HIGH);
    }

    @Test
    void ruleAuditAction_containsExpectedValues() {
        assertThat(RuleAuditAction.values())
                .containsExactly(
                        RuleAuditAction.CREATED,
                        RuleAuditAction.UPDATED,
                        RuleAuditAction.ACTIVATED,
                        RuleAuditAction.DEACTIVATED,
                        RuleAuditAction.DELETED);
        assertThat(RuleAuditAction.valueOf("DELETED")).isEqualTo(RuleAuditAction.DELETED);
    }

    @Test
    void ruleExecutionOutcome_containsExpectedValues() {
        assertThat(RuleExecutionOutcome.values())
                .containsExactly(RuleExecutionOutcome.TRIGGERED, RuleExecutionOutcome.NOT_TRIGGERED);
        assertThat(RuleExecutionOutcome.valueOf("TRIGGERED")).isEqualTo(RuleExecutionOutcome.TRIGGERED);
    }
}

