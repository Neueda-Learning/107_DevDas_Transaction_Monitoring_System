package com.hsbc.tms.alerts.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertEntitiesTest {

    @Test
    void alert_gettersAndSettersWork() {
        Instant now = Instant.parse("2026-08-06T10:00:00Z");
        UUID txnId = UUID.randomUUID();

        Alert alert = new Alert();
        alert.setId(4L);
        alert.setRuleName("Velocity Rule");
        alert.setRuleType(RuleType.VELOCITY);
        alert.setSeverity(AlertSeverity.MEDIUM);
        alert.setStatus(AlertStatus.INVESTIGATING);
        alert.setMessage("Velocity alert");
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now.plusSeconds(30));
        alert.setTriggeringTransactionIds(List.of(txnId));

        assertThat(alert.getId()).isEqualTo(4L);
        assertThat(alert.getRuleName()).isEqualTo("Velocity Rule");
        assertThat(alert.getRuleType()).isEqualTo(RuleType.VELOCITY);
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.INVESTIGATING);
        assertThat(alert.getMessage()).isEqualTo("Velocity alert");
        assertThat(alert.getCreatedAt()).isEqualTo(now);
        assertThat(alert.getUpdatedAt()).isEqualTo(now.plusSeconds(30));
        assertThat(alert.getTriggeringTransactionIds()).containsExactly(txnId);
    }

    @Test
    void alertHistory_gettersAndSettersWork() {
        Instant now = Instant.parse("2026-08-06T11:00:00Z");

        AlertHistory history = new AlertHistory();
        history.setId(7L);
        history.setAlertId(4L);
        history.setFromStatus(AlertStatus.OPEN);
        history.setToStatus(AlertStatus.CLOSED);
        history.setNote("resolved");
        history.setChangedBy("analyst-9");
        history.setCreatedAt(now);

        assertThat(history.getId()).isEqualTo(7L);
        assertThat(history.getAlertId()).isEqualTo(4L);
        assertThat(history.getFromStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(history.getToStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(history.getNote()).isEqualTo("resolved");
        assertThat(history.getChangedBy()).isEqualTo("analyst-9");
        assertThat(history.getCreatedAt()).isEqualTo(now);
    }
}

