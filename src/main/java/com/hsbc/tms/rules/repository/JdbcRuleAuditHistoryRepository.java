package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.RuleAuditHistory;
import com.hsbc.tms.rules.model.RuleAuditAction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRuleAuditHistoryRepository implements RuleAuditHistoryRepository {

    private static final RowMapper<RuleAuditHistory> RULE_AUDIT_ROW_MAPPER = new RuleAuditHistoryRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcRuleAuditHistoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(RuleAuditHistory history) {
        String sql = """
                INSERT INTO rule_audit_history (
                    rule_id, action, previous_values, new_values, changed_at, changed_by
                ) VALUES (
                    :ruleId, :action, :previousValues, :newValues, :changedAt, :changedBy
                )
                """;

        jdbcClient.sql(sql)
                .param("ruleId", history.getRuleId())
                .param("action", history.getAction().name())
                .param("previousValues", history.getPreviousValues())
                .param("newValues", history.getNewValues())
                .param("changedAt", Timestamp.from(history.getChangedAt()))
                .param("changedBy", history.getChangedBy())
                .update();
    }

    @Override
    public List<RuleAuditHistory> findByRuleIdOrderByChangedAtAsc(Long ruleId) {
        String sql = """
                SELECT id, rule_id, action, previous_values, new_values, changed_at, changed_by
                FROM rule_audit_history
                WHERE rule_id = :ruleId
                ORDER BY changed_at ASC, id ASC
                """;

        return jdbcClient.sql(sql).param("ruleId", ruleId).query(RULE_AUDIT_ROW_MAPPER).list();
    }

    private static final class RuleAuditHistoryRowMapper implements RowMapper<RuleAuditHistory> {

        @Override
        public RuleAuditHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            RuleAuditHistory history = new RuleAuditHistory();
            history.setId(rs.getLong("id"));
            history.setRuleId(rs.getLong("rule_id"));
            history.setAction(RuleAuditAction.valueOf(rs.getString("action")));
            history.setPreviousValues(rs.getString("previous_values"));
            history.setNewValues(rs.getString("new_values"));
            Timestamp changedAt = rs.getTimestamp("changed_at");
            history.setChangedAt(changedAt == null ? null : changedAt.toInstant());
            history.setChangedBy(rs.getString("changed_by"));
            return history;
        }
    }
}

