package com.hsbc.tms.rules.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.dto.RuleAuditHistoryResponse;
import com.hsbc.tms.rules.dto.RuleResponse;
import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.entity.RuleAuditHistory;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleAuditAction;
import com.hsbc.tms.rules.model.RuleType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RuleApiMapperTest {

    @Test
    void toRuleResponse_mapsAllFields() {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(11L);
        rule.setName("Velocity Rule");
        rule.setType(RuleType.VELOCITY);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setActive(true);
        rule.setAmountThreshold(null);
        rule.setTransactionCountThreshold(5);
        rule.setTimeWindowMinutes(10);

        RuleResponse response = RuleApiMapper.toRuleResponse(rule);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.name()).isEqualTo("Velocity Rule");
        assertThat(response.type()).isEqualTo(RuleType.VELOCITY);
        assertThat(response.severity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(response.active()).isTrue();
        assertThat(response.amountThreshold()).isNull();
        assertThat(response.transactionCountThreshold()).isEqualTo(5);
        assertThat(response.timeWindowMinutes()).isEqualTo(10);
    }

    @Test
    void toRuleAuditHistoryResponse_mapsAllFields() {
        Instant changedAt = Instant.parse("2026-08-05T10:15:30Z");
        RuleAuditHistory history = new RuleAuditHistory();
        history.setId(7L);
        history.setRuleId(99L);
        history.setAction(RuleAuditAction.UPDATED);
        history.setPreviousValues("old");
        history.setNewValues("new");
        history.setChangedAt(changedAt);
        history.setChangedBy("SYSTEM");

        RuleAuditHistoryResponse response = RuleApiMapper.toRuleAuditHistoryResponse(history);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.ruleId()).isEqualTo(99L);
        assertThat(response.action()).isEqualTo(RuleAuditAction.UPDATED);
        assertThat(response.previousValues()).isEqualTo("old");
        assertThat(response.newValues()).isEqualTo("new");
        assertThat(response.changedAt()).isEqualTo(changedAt);
        assertThat(response.changedBy()).isEqualTo("SYSTEM");
    }
}

