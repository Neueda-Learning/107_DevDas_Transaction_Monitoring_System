package com.hsbc.tms.alerts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.alerts.entity.Alert;
import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcAlertRepository.class)
@ActiveProfiles("test")
class JdbcAlertRepositoryTest {

    @Autowired
    private JdbcAlertRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM alert_transactions").update();
        jdbcClient.sql("DELETE FROM alert_history").update();
        jdbcClient.sql("DELETE FROM alerts").update();
    }

    @Test
    void search_appliesStatusSeverityAndActiveOnlyFilters() {
        Long openId = insertAlert("Rule Open", RuleType.VELOCITY, AlertSeverity.HIGH, AlertStatus.OPEN, "m1", Instant.parse("2026-08-06T10:00:00Z"));
        Long closedId = insertAlert("Rule Closed", RuleType.NEW_PAYEE, AlertSeverity.HIGH, AlertStatus.CLOSED, "m2", Instant.parse("2026-08-06T11:00:00Z"));
        Long acknowledgedId = insertAlert("Rule Ack", RuleType.DAILY_LIMIT, AlertSeverity.LOW, AlertStatus.ACKNOWLEDGED, "m3", Instant.parse("2026-08-06T12:00:00Z"));

        Set<AlertStatus> activeStatuses = Set.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING);

        List<Alert> activeOnlyRows = repository.search(null, null, true, activeStatuses);
        assertThat(activeOnlyRows).extracting(Alert::getId).containsExactly(acknowledgedId, openId);

        List<Alert> filteredRows = repository.search(AlertStatus.CLOSED, AlertSeverity.HIGH, false, activeStatuses);
        assertThat(filteredRows).hasSize(1);
        assertThat(filteredRows.get(0).getId()).isEqualTo(closedId);
    }

    @Test
    void findById_returnsAlertWithTriggeringTransactionIds() {
        Long alertId = insertAlert("Rule", RuleType.AMOUNT_THRESHOLD, AlertSeverity.MEDIUM, AlertStatus.OPEN, "msg", Instant.parse("2026-08-06T10:30:00Z"));
        UUID t1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID t2 = UUID.fromString("00000000-0000-0000-0000-000000000020");
        insertAlertTransaction(alertId, t2);
        insertAlertTransaction(alertId, t1);

        Alert row = repository.findById(alertId).orElseThrow();

        assertThat(row.getId()).isEqualTo(alertId);
        assertThat(row.getRuleName()).isEqualTo("Rule");
        assertThat(row.getTriggeringTransactionIds()).containsExactly(t1, t2);
    }

    @Test
    void existsById_returnsExpectedValues() {
        Long alertId = insertAlert("Rule", RuleType.AMOUNT_THRESHOLD, AlertSeverity.HIGH, AlertStatus.OPEN, "msg", Instant.parse("2026-08-06T10:00:00Z"));

        assertThat(repository.existsById(alertId)).isTrue();
        assertThat(repository.existsById(99999L)).isFalse();
    }

    @Test
    void save_insertsNewAlert() {
        Instant now = Instant.parse("2026-08-06T09:00:00Z");

        Alert alert = new Alert();
        alert.setRuleName("Insert Rule");
        alert.setRuleType(RuleType.NEW_PAYEE);
        alert.setSeverity(AlertSeverity.MEDIUM);
        alert.setStatus(AlertStatus.OPEN);
        alert.setMessage("inserted");
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);

        Alert saved = repository.save(alert);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void save_updatesExistingAlert() {
        Long alertId = insertAlert("Before", RuleType.NEW_PAYEE, AlertSeverity.LOW, AlertStatus.OPEN, "old", Instant.parse("2026-08-06T08:00:00Z"));

        Alert existing = repository.findById(alertId).orElseThrow();
        existing.setRuleName("After");
        existing.setRuleType(RuleType.VELOCITY);
        existing.setSeverity(AlertSeverity.HIGH);
        existing.setStatus(AlertStatus.INVESTIGATING);
        existing.setMessage("new");
        existing.setUpdatedAt(Instant.parse("2026-08-06T08:30:00Z"));

        repository.save(existing);

        Alert updated = repository.findById(alertId).orElseThrow();
        assertThat(updated.getRuleName()).isEqualTo("After");
        assertThat(updated.getRuleType()).isEqualTo(RuleType.VELOCITY);
        assertThat(updated.getSeverity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(updated.getStatus()).isEqualTo(AlertStatus.INVESTIGATING);
        assertThat(updated.getMessage()).isEqualTo("new");
    }

    @Test
    void replaceTriggeringTransactions_overwritesAndDeduplicates() {
        Long alertId = insertAlert("Rule", RuleType.NEW_PAYEE, AlertSeverity.HIGH, AlertStatus.OPEN, "msg", Instant.parse("2026-08-06T07:00:00Z"));
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

        repository.replaceTriggeringTransactions(alertId, List.of(id1, id2, id1));

        List<UUID> rows = repository.findTriggeringTransactionIdsByAlertId(alertId);
        assertThat(rows).containsExactly(id1, id2);
    }

    @Test
    void findActiveByTriggeringTransactionId_returnsOnlyActiveAlerts() {
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-00000000f001");
        Long openId = insertAlert("Rule Open", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, AlertStatus.OPEN, "m1", Instant.parse("2026-08-06T10:00:00Z"));
        Long closedId = insertAlert("Rule Closed", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, AlertStatus.CLOSED, "m2", Instant.parse("2026-08-06T11:00:00Z"));
        Long invId = insertAlert("Rule Investigating", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, AlertStatus.INVESTIGATING, "m3", Instant.parse("2026-08-06T12:00:00Z"));
        insertAlertTransaction(openId, transactionId);
        insertAlertTransaction(closedId, transactionId);
        insertAlertTransaction(invId, transactionId);

        List<Alert> rows = repository.findActiveByTriggeringTransactionId(
                transactionId,
                Set.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING));

        assertThat(rows).extracting(Alert::getStatus).containsExactly(AlertStatus.INVESTIGATING, AlertStatus.OPEN);
        assertThat(rows).allMatch(a -> !a.getTriggeringTransactionIds().isEmpty());
    }

    private Long insertAlert(
            String ruleName,
            RuleType ruleType,
            AlertSeverity severity,
            AlertStatus status,
            String message,
            Instant timestamp) {
        String sql = """
                INSERT INTO alerts (
                    rule_name, rule_type, severity, status, message, created_at, updated_at
                ) VALUES (
                    :ruleName, :ruleType, :severity, :status, :message, :createdAt, :updatedAt
                )
                """;

        jdbcClient.sql(sql)
                .param("ruleName", ruleName)
                .param("ruleType", ruleType.name())
                .param("severity", severity.name())
                .param("status", status.name())
                .param("message", message)
                .param("createdAt", Timestamp.from(timestamp))
                .param("updatedAt", Timestamp.from(timestamp))
                .update();

        return jdbcClient.sql("SELECT id FROM alerts WHERE rule_name = :ruleName")
                .param("ruleName", ruleName)
                .query(Long.class)
                .single();
    }

    private void insertAlertTransaction(Long alertId, UUID transactionId) {
        jdbcClient.sql("INSERT INTO alert_transactions (alert_id, transaction_id) VALUES (:alertId, :transactionId)")
                .param("alertId", alertId)
                .param("transactionId", transactionId.toString())
                .update();
    }
}
