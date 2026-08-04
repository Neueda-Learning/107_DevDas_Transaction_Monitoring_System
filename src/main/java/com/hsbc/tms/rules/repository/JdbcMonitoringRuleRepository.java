package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMonitoringRuleRepository implements MonitoringRuleRepository {

    private static final RowMapper<MonitoringRule> RULE_ROW_MAPPER = new MonitoringRuleRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcMonitoringRuleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<MonitoringRule> findByActiveTrue() {
        String sql = """
                SELECT id, name, type, severity, active, amount_threshold,
                       transaction_count_threshold, time_window_minutes, created_at
                FROM monitoring_rules
                WHERE active = true
                ORDER BY id ASC
                """;
        return jdbcClient.sql(sql).query(RULE_ROW_MAPPER).list();
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        String sql = "SELECT COUNT(*) FROM monitoring_rules WHERE LOWER(name) = LOWER(:name)";
        Long count = jdbcClient.sql(sql).param("name", name).query(Long.class).single();
        return count > 0;
    }

    @Override
    public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
        String sql = "SELECT COUNT(*) FROM monitoring_rules WHERE LOWER(name) = LOWER(:name) AND id <> :id";
        Long count = jdbcClient.sql(sql).param("name", name).param("id", id).query(Long.class).single();
        return count > 0;
    }

    @Override
    public long count() {
        return jdbcClient.sql("SELECT COUNT(*) FROM monitoring_rules").query(Long.class).single();
    }

    @Override
    public long countByActiveTrue() {
        return jdbcClient.sql("SELECT COUNT(*) FROM monitoring_rules WHERE active = true").query(Long.class).single();
    }

    @Override
    public long countByActiveFalse() {
        return jdbcClient.sql("SELECT COUNT(*) FROM monitoring_rules WHERE active = false").query(Long.class).single();
    }

    @Override
    public Map<RuleType, Long> countGroupedByType() {
        Map<RuleType, Long> grouped = new EnumMap<>(RuleType.class);
        for (RuleType type : RuleType.values()) {
            grouped.put(type, 0L);
        }

        String sql = "SELECT type, COUNT(*) AS cnt FROM monitoring_rules GROUP BY type";
        List<Map<String, Object>> rows = jdbcClient.sql(sql).query().listOfRows();
        for (Map<String, Object> row : rows) {
            RuleType type = RuleType.valueOf((String) row.get("type"));
            Number count = (Number) row.get("cnt");
            grouped.put(type, count.longValue());
        }

        return grouped;
    }

    @Override
    public Map<AlertSeverity, Long> countGroupedBySeverity() {
        Map<AlertSeverity, Long> grouped = new EnumMap<>(AlertSeverity.class);
        for (AlertSeverity severity : AlertSeverity.values()) {
            grouped.put(severity, 0L);
        }

        String sql = "SELECT severity, COUNT(*) AS cnt FROM monitoring_rules GROUP BY severity";
        List<Map<String, Object>> rows = jdbcClient.sql(sql).query().listOfRows();
        for (Map<String, Object> row : rows) {
            AlertSeverity severity = AlertSeverity.valueOf((String) row.get("severity"));
            Number count = (Number) row.get("cnt");
            grouped.put(severity, count.longValue());
        }

        return grouped;
    }

    @Override
    public List<MonitoringRule> search(Boolean active, RuleType type, AlertSeverity severity) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, type, severity, active, amount_threshold,
                       transaction_count_threshold, time_window_minutes, created_at
                FROM monitoring_rules
                WHERE 1=1
                """);
        Map<String, Object> params = new HashMap<>();

        if (active != null) {
            sql.append(" AND active = :active");
            params.put("active", active);
        }
        if (type != null) {
            sql.append(" AND type = :type");
            params.put("type", type.name());
        }
        if (severity != null) {
            sql.append(" AND severity = :severity");
            params.put("severity", severity.name());
        }

        sql.append(" ORDER BY id ASC");
        return jdbcClient.sql(sql.toString()).params(params).query(RULE_ROW_MAPPER).list();
    }

    @Override
    public Optional<MonitoringRule> findById(Long id) {
        String sql = """
                SELECT id, name, type, severity, active, amount_threshold,
                       transaction_count_threshold, time_window_minutes, created_at
                FROM monitoring_rules
                WHERE id = :id
                """;
        try {
            MonitoringRule rule = jdbcClient.sql(sql).param("id", id).query(RULE_ROW_MAPPER).single();
            return Optional.of(rule);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM monitoring_rules WHERE id = :id";
        Long count = jdbcClient.sql(sql).param("id", id).query(Long.class).single();
        return count > 0;
    }

    @Override
    public MonitoringRule save(MonitoringRule rule) {
        if (rule.getId() == null) {
            if (rule.getCreatedAt() == null) {
                rule.setCreatedAt(Instant.now());
            }

            String insertSql = """
                    INSERT INTO monitoring_rules (
                        name, type, severity, active, amount_threshold,
                        transaction_count_threshold, time_window_minutes, created_at
                    ) VALUES (
                        :name, :type, :severity, :active, :amountThreshold,
                        :transactionCountThreshold, :timeWindowMinutes, :createdAt
                    )
                    """;
            jdbcClient.sql(insertSql).params(toParams(rule)).update();

            Long id = jdbcClient.sql("SELECT id FROM monitoring_rules WHERE LOWER(name) = LOWER(:name)")
                    .param("name", rule.getName())
                    .query(Long.class)
                    .single();
            rule.setId(id);
            return rule;
        }

        String updateSql = """
                UPDATE monitoring_rules
                SET name = :name,
                    type = :type,
                    severity = :severity,
                    active = :active,
                    amount_threshold = :amountThreshold,
                    transaction_count_threshold = :transactionCountThreshold,
                    time_window_minutes = :timeWindowMinutes
                WHERE id = :id
                """;
        jdbcClient.sql(updateSql).params(toParams(rule)).update();
        return rule;
    }

    private Map<String, Object> toParams(MonitoringRule rule) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", rule.getId());
        params.put("name", rule.getName());
        params.put("type", rule.getType() == null ? null : rule.getType().name());
        params.put("severity", rule.getSeverity() == null ? null : rule.getSeverity().name());
        params.put("active", rule.isActive());
        params.put("amountThreshold", rule.getAmountThreshold());
        params.put("transactionCountThreshold", rule.getTransactionCountThreshold());
        params.put("timeWindowMinutes", rule.getTimeWindowMinutes());
        params.put("createdAt", rule.getCreatedAt() == null ? null : Timestamp.from(rule.getCreatedAt()));
        return params;
    }

    private static final class MonitoringRuleRowMapper implements RowMapper<MonitoringRule> {

        @Override
        public MonitoringRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            MonitoringRule rule = new MonitoringRule();
            rule.setId(rs.getLong("id"));
            rule.setName(rs.getString("name"));
            rule.setType(RuleType.valueOf(rs.getString("type")));
            rule.setSeverity(AlertSeverity.valueOf(rs.getString("severity")));
            rule.setActive(rs.getBoolean("active"));
            rule.setAmountThreshold(rs.getBigDecimal("amount_threshold"));
            rule.setTransactionCountThreshold((Integer) rs.getObject("transaction_count_threshold"));
            rule.setTimeWindowMinutes((Integer) rs.getObject("time_window_minutes"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            rule.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
            return rule;
        }
    }
}

