package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.RuleExecutionHistory;
import com.hsbc.tms.rules.model.RuleExecutionOutcome;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRuleExecutionHistoryRepository implements RuleExecutionHistoryRepository {

    private static final RowMapper<RuleExecutionHistory> ROW_MAPPER = new RuleExecutionHistoryRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcRuleExecutionHistoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(RuleExecutionHistory history) {
        String sql = """
                INSERT INTO rule_execution_history (
                    execution_id, rule_id, transaction_id, outcome, message, created_at
                ) VALUES (
                    :executionId, :ruleId, :transactionId, :outcome, :message, :createdAt
                )
                """;

        jdbcClient.sql(sql)
                .param("executionId", history.getExecutionId().toString())
                .param("ruleId", history.getRuleId())
                .param("transactionId", history.getTransactionId().toString())
                .param("outcome", history.getOutcome().name())
                .param("message", history.getMessage())
                .param("createdAt", Timestamp.from(history.getCreatedAt()))
                .update();
    }

    @Override
    public List<RuleExecutionHistory> findByTransactionId(UUID transactionId) {
        String sql = """
                SELECT id, execution_id, rule_id, transaction_id, outcome, message, created_at
                FROM rule_execution_history
                WHERE transaction_id = :transactionId
                ORDER BY created_at ASC, id ASC
                """;

        return jdbcClient.sql(sql)
                .param("transactionId", transactionId.toString())
                .query(ROW_MAPPER)
                .list();
    }

    private static final class RuleExecutionHistoryRowMapper implements RowMapper<RuleExecutionHistory> {

        @Override
        public RuleExecutionHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            RuleExecutionHistory history = new RuleExecutionHistory();
            history.setId(rs.getLong("id"));
            history.setExecutionId(UUID.fromString(rs.getString("execution_id")));
            history.setRuleId(rs.getLong("rule_id"));
            history.setTransactionId(UUID.fromString(rs.getString("transaction_id")));
            history.setOutcome(RuleExecutionOutcome.valueOf(rs.getString("outcome")));
            history.setMessage(rs.getString("message"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            history.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
            return history;
        }
    }
}

