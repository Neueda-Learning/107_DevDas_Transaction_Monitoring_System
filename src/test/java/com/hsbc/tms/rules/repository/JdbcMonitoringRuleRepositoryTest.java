package com.hsbc.tms.rules.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
@Import(JdbcMonitoringRuleRepository.class)
@ActiveProfiles("test")
class JdbcMonitoringRuleRepositoryTest {

    @Autowired
    private JdbcMonitoringRuleRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM monitoring_rules").update();
    }

    @Test
    void findByActiveTrue_returnsOnlyActiveRows() {
        insertRule("Rule Active", RuleType.AMOUNT_THRESHOLD, AlertSeverity.HIGH, true, new BigDecimal("1000.00"), null, null);
        insertRule("Rule Inactive", RuleType.NEW_PAYEE, AlertSeverity.LOW, false, null, null, null);

        List<MonitoringRule> rows = repository.findByActiveTrue();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getName()).isEqualTo("Rule Active");
    }

    @Test
    void existsMethods_returnExpectedValues() {
        Long id = insertRule("Velocity A", RuleType.VELOCITY, AlertSeverity.MEDIUM, true, null, 5, 10);

        assertThat(repository.existsByNameIgnoreCase("velocity a")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("missing")).isFalse();
        assertThat(repository.existsByNameIgnoreCaseAndIdNot("Velocity A", id + 1)).isTrue();
        assertThat(repository.existsByNameIgnoreCaseAndIdNot("Velocity A", id)).isFalse();
        assertThat(repository.existsById(id)).isTrue();
        assertThat(repository.existsById(99999L)).isFalse();
    }

    @Test
    void countMethodsAndGroupedCounts_returnExpectedValues() {
        insertRule("A", RuleType.AMOUNT_THRESHOLD, AlertSeverity.HIGH, true, new BigDecimal("100.00"), null, null);
        insertRule("B", RuleType.VELOCITY, AlertSeverity.MEDIUM, true, null, 5, 10);
        insertRule("C", RuleType.NEW_PAYEE, AlertSeverity.MEDIUM, false, null, null, null);

        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.countByActiveTrue()).isEqualTo(2);
        assertThat(repository.countByActiveFalse()).isEqualTo(1);

        Map<RuleType, Long> byType = repository.countGroupedByType();
        assertThat(byType.get(RuleType.AMOUNT_THRESHOLD)).isEqualTo(1);
        assertThat(byType.get(RuleType.VELOCITY)).isEqualTo(1);
        assertThat(byType.get(RuleType.NEW_PAYEE)).isEqualTo(1);
        assertThat(byType.get(RuleType.DAILY_LIMIT)).isZero();

        Map<AlertSeverity, Long> bySeverity = repository.countGroupedBySeverity();
        assertThat(bySeverity.get(AlertSeverity.HIGH)).isEqualTo(1);
        assertThat(bySeverity.get(AlertSeverity.MEDIUM)).isEqualTo(2);
        assertThat(bySeverity.get(AlertSeverity.LOW)).isZero();
    }

    @Test
    void search_appliesFilters() {
        insertRule("A", RuleType.AMOUNT_THRESHOLD, AlertSeverity.HIGH, true, new BigDecimal("100.00"), null, null);
        insertRule("B", RuleType.VELOCITY, AlertSeverity.MEDIUM, true, null, 5, 10);
        insertRule("C", RuleType.VELOCITY, AlertSeverity.HIGH, false, null, 3, 5);

        List<MonitoringRule> filtered = repository.search(true, RuleType.VELOCITY, AlertSeverity.MEDIUM);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getName()).isEqualTo("B");
    }

    @Test
    void findById_returnsOptionalValue() {
        Long id = insertRule("Find Me", RuleType.NEW_PAYEE, AlertSeverity.LOW, true, null, null, null);

        assertThat(repository.findById(id)).isPresent();
        assertThat(repository.findById(123456L)).isEmpty();
    }

    @Test
    void save_insertsNewRuleAndReturnsId() {
        MonitoringRule rule = new MonitoringRule();
        rule.setName("Inserted Rule");
        rule.setType(RuleType.DAILY_LIMIT);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setActive(true);
        rule.setAmountThreshold(new BigDecimal("5000.00"));

        MonitoringRule saved = repository.save(rule);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void save_updatesExistingRule() {
        Long id = insertRule("Old Name", RuleType.NEW_PAYEE, AlertSeverity.LOW, true, null, null, null);

        MonitoringRule existing = repository.findById(id).orElseThrow();
        existing.setName("Updated Name");
        existing.setType(RuleType.VELOCITY);
        existing.setSeverity(AlertSeverity.MEDIUM);
        existing.setActive(false);
        existing.setAmountThreshold(null);
        existing.setTransactionCountThreshold(7);
        existing.setTimeWindowMinutes(12);

        repository.save(existing);

        MonitoringRule updated = repository.findById(id).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getType()).isEqualTo(RuleType.VELOCITY);
        assertThat(updated.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getTransactionCountThreshold()).isEqualTo(7);
        assertThat(updated.getTimeWindowMinutes()).isEqualTo(12);
    }

    private Long insertRule(
            String name,
            RuleType type,
            AlertSeverity severity,
            boolean active,
            BigDecimal amountThreshold,
            Integer transactionCountThreshold,
            Integer timeWindowMinutes) {
        String sql = """
                INSERT INTO monitoring_rules (
                    name, type, severity, active, amount_threshold,
                    transaction_count_threshold, time_window_minutes, created_at
                ) VALUES (
                    :name, :type, :severity, :active, :amountThreshold,
                    :transactionCountThreshold, :timeWindowMinutes, :createdAt
                )
                """;

        jdbcClient.sql(sql)
                .param("name", name)
                .param("type", type.name())
                .param("severity", severity.name())
                .param("active", active)
                .param("amountThreshold", amountThreshold)
                .param("transactionCountThreshold", transactionCountThreshold)
                .param("timeWindowMinutes", timeWindowMinutes)
                .param("createdAt", Timestamp.from(Instant.parse("2026-08-05T00:00:00Z")))
                .update();

        return jdbcClient.sql("SELECT id FROM monitoring_rules WHERE name = :name")
                .param("name", name)
                .query(Long.class)
                .single();
    }
}

