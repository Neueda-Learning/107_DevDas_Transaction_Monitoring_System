package com.hsbc.tms.alerts.repository;

import com.hsbc.tms.alerts.entity.Alert;
import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAlertRepository implements AlertRepository {

    private static final RowMapper<Alert> ALERT_ROW_MAPPER = new AlertRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcAlertRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Alert> search(AlertStatus status, AlertSeverity severity, boolean activeOnly, Set<AlertStatus> activeStatuses) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, rule_name, rule_type, severity, status, message, created_at, updated_at
                FROM alerts
                WHERE 1=1
                """);
        Map<String, Object> params = new HashMap<>();

        if (status != null) {
            sql.append(" AND status = :status");
            params.put("status", status.name());
        }
        if (severity != null) {
            sql.append(" AND severity = :severity");
            params.put("severity", severity.name());
        }
        if (activeOnly) {
            sql.append(" AND status IN (:activeStatuses)");
            params.put("activeStatuses", activeStatuses.stream().map(Enum::name).toList());
        }

        sql.append(" ORDER BY created_at DESC, id DESC");
        List<Alert> alerts = jdbcClient.sql(sql.toString()).params(params).query(ALERT_ROW_MAPPER).list();
        hydrateTriggeringTransactionIds(alerts);
        return alerts;
    }

    @Override
    public Optional<Alert> findById(Long id) {
        String sql = """
                SELECT id, rule_name, rule_type, severity, status, message, created_at, updated_at
                FROM alerts
                WHERE id = :id
                """;
        try {
            Alert alert = jdbcClient.sql(sql).param("id", id).query(ALERT_ROW_MAPPER).single();
            alert.setTriggeringTransactionIds(findTriggeringTransactionIdsByAlertId(alert.getId()));
            return Optional.of(alert);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM alerts WHERE id = :id";
        Long count = jdbcClient.sql(sql).param("id", id).query(Long.class).single();
        return count > 0;
    }

    @Override
    public Alert save(Alert alert) {
        if (alert.getId() == null) {
            String insertSql = """
                    INSERT INTO alerts (
                        rule_name, rule_type, severity, status, message, created_at, updated_at
                    ) VALUES (
                        :ruleName, :ruleType, :severity, :status, :message, :createdAt, :updatedAt
                    )
                    """;
            jdbcClient.sql(insertSql).params(toParams(alert)).update();

            String findSql = """
                    SELECT id
                    FROM alerts
                    WHERE rule_name = :ruleName
                      AND status = :status
                      AND created_at = :createdAt
                    ORDER BY id DESC
                    LIMIT 1
                    """;
            Long id = jdbcClient.sql(findSql)
                    .param("ruleName", alert.getRuleName())
                    .param("status", alert.getStatus().name())
                    .param("createdAt", Timestamp.from(alert.getCreatedAt()))
                    .query(Long.class)
                    .single();
            alert.setId(id);
            return alert;
        }

        String updateSql = """
                UPDATE alerts
                SET rule_name = :ruleName,
                    rule_type = :ruleType,
                    severity = :severity,
                    status = :status,
                    message = :message,
                    created_at = :createdAt,
                    updated_at = :updatedAt
                WHERE id = :id
                """;
        jdbcClient.sql(updateSql).params(toParams(alert)).update();
        return alert;
    }

    @Override
    public void replaceTriggeringTransactions(Long alertId, List<UUID> triggeringTransactionIds) {
        jdbcClient.sql("DELETE FROM alert_transactions WHERE alert_id = :alertId")
                .param("alertId", alertId)
                .update();

        String insertSql = """
                INSERT INTO alert_transactions (alert_id, transaction_id)
                VALUES (:alertId, :transactionId)
                """;

        LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>(triggeringTransactionIds);
        for (UUID transactionId : uniqueIds) {
            jdbcClient.sql(insertSql)
                    .param("alertId", alertId)
                    .param("transactionId", transactionId.toString())
                    .update();
        }
    }

    @Override
    public List<UUID> findTriggeringTransactionIdsByAlertId(Long alertId) {
        String sql = """
                SELECT transaction_id
                FROM alert_transactions
                WHERE alert_id = :alertId
                ORDER BY transaction_id ASC
                """;

        return jdbcClient.sql(sql)
                .param("alertId", alertId)
                .query(String.class)
                .list()
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    @Override
    public List<Alert> findActiveByTriggeringTransactionId(UUID transactionId, Set<AlertStatus> activeStatuses) {
        String sql = """
                SELECT a.id, a.rule_name, a.rule_type, a.severity, a.status, a.message, a.created_at, a.updated_at
                FROM alerts a
                JOIN alert_transactions atx ON atx.alert_id = a.id
                WHERE atx.transaction_id = :transactionId
                  AND a.status IN (:activeStatuses)
                ORDER BY a.created_at DESC, a.id DESC
                """;

        List<Alert> alerts = jdbcClient.sql(sql)
                .param("transactionId", transactionId.toString())
                .param("activeStatuses", activeStatuses.stream().map(Enum::name).toList())
                .query(ALERT_ROW_MAPPER)
                .list();
        hydrateTriggeringTransactionIds(alerts);
        return alerts;
    }

    private Map<String, Object> toParams(Alert alert) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", alert.getId());
        params.put("ruleName", alert.getRuleName());
        params.put("ruleType", alert.getRuleType() == null ? null : alert.getRuleType().name());
        params.put("severity", alert.getSeverity() == null ? null : alert.getSeverity().name());
        params.put("status", alert.getStatus() == null ? null : alert.getStatus().name());
        params.put("message", alert.getMessage());
        params.put("createdAt", alert.getCreatedAt() == null ? null : Timestamp.from(alert.getCreatedAt()));
        params.put("updatedAt", alert.getUpdatedAt() == null ? null : Timestamp.from(alert.getUpdatedAt()));
        return params;
    }

    private void hydrateTriggeringTransactionIds(List<Alert> alerts) {
        for (Alert alert : alerts) {
            alert.setTriggeringTransactionIds(findTriggeringTransactionIdsByAlertId(alert.getId()));
        }
    }

    private static final class AlertRowMapper implements RowMapper<Alert> {

        @Override
        public Alert mapRow(ResultSet rs, int rowNum) throws SQLException {
            Alert alert = new Alert();
            alert.setId(rs.getLong("id"));
            alert.setRuleName(rs.getString("rule_name"));
            alert.setRuleType(RuleType.valueOf(rs.getString("rule_type")));
            alert.setSeverity(AlertSeverity.valueOf(rs.getString("severity")));
            alert.setStatus(AlertStatus.valueOf(rs.getString("status")));
            alert.setMessage(rs.getString("message"));
            alert.setCreatedAt(getInstant(rs, "created_at"));
            alert.setUpdatedAt(getInstant(rs, "updated_at"));
            return alert;
        }

        private Instant getInstant(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}


