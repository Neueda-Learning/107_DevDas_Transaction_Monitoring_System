package com.hsbc.tms.rules.repository;

import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRuleTransactionMetricsRepository implements RuleTransactionMetricsRepository {

    private static final RowMapper<Transaction> TRANSACTION_ROW_MAPPER = new TransactionRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcRuleTransactionMetricsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public long countByAccountIdAndTransactionTimeBetween(String accountId, Instant fromTime, Instant toTime) {
        String sql = """
                SELECT COUNT(*)
                FROM transactions
                WHERE account_id = :accountId
                  AND transaction_time >= :fromTime
                  AND transaction_time <= :toTime
                """;

        return jdbcClient.sql(sql)
                .param("accountId", accountId)
                .param("fromTime", Timestamp.from(fromTime))
                .param("toTime", Timestamp.from(toTime))
                .query(Long.class)
                .single();
    }



    @Override
    public List<Transaction> findByAccountIdAndTransactionTimeBetween(String accountId, Instant fromTime, Instant toTime) {
        String sql = """
                SELECT id, account_id, payee_id, amount, currency, type, status,
                       transaction_time, description, created_at, updated_at
                FROM transactions
                WHERE account_id = :accountId
                  AND transaction_time >= :fromTime
                  AND transaction_time <= :toTime
                ORDER BY transaction_time ASC
                """;

        return jdbcClient.sql(sql)
                .param("accountId", accountId)
                .param("fromTime", Timestamp.from(fromTime))
                .param("toTime", Timestamp.from(toTime))
                .query(TRANSACTION_ROW_MAPPER)
                .list();
    }

    @Override
    public List<Transaction> findByAccountIdAndTransactionTimeBetweenAndStatus(String accountId, Instant fromTime, Instant toTime, TransactionStatus status) {
        String sql = """
                SELECT id, account_id, payee_id, amount, currency, type, status,
                       transaction_time, description, created_at, updated_at
                FROM transactions
                WHERE account_id = :accountId
                  AND transaction_time >= :fromTime
                  AND transaction_time <= :toTime
                  AND status = :status
                ORDER BY transaction_time ASC
                """;

        return jdbcClient.sql(sql)
                .param("accountId", accountId)
                .param("fromTime", Timestamp.from(fromTime))
                .param("toTime", Timestamp.from(toTime))
                .param("status", status.name())
                .query(TRANSACTION_ROW_MAPPER)
                .list();
    }

    @Override
    public long countByAccountIdAndPayeeIdAndTransactionTimeBefore(String accountId, String payeeId, Instant beforeTime) {
        String sql = """
                SELECT COUNT(*)
                FROM transactions
                WHERE account_id = :accountId
                  AND payee_id = :payeeId
                  AND transaction_time < :beforeTime
                """;

        return jdbcClient.sql(sql)
                .param("accountId", accountId)
                .param("payeeId", payeeId)
                .param("beforeTime", Timestamp.from(beforeTime))
                .query(Long.class)
                .single();
    }

    @Override
    public BigDecimal sumAmountByAccountAndTransactionTimeRange(String accountId, Instant fromTime, Instant toTime) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM transactions
                WHERE account_id = :accountId
                  AND transaction_time >= :fromTime
                  AND transaction_time <= :toTime
                """;

        BigDecimal total = jdbcClient.sql(sql)
                .param("accountId", accountId)
                .param("fromTime", Timestamp.from(fromTime))
                .param("toTime", Timestamp.from(toTime))
                .query(BigDecimal.class)
                .single();
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public BigDecimal sumAmountByAccountAndTransactionTimeRangeAndStatus(String accountId, Instant fromTime, Instant toTime, TransactionStatus status) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM transactions
                WHERE account_id = :accountId
                  AND transaction_time >= :fromTime
                  AND transaction_time <= :toTime
                  AND status = :status
                """;

        BigDecimal total = jdbcClient.sql(sql)
                .param("accountId", accountId)
                .param("fromTime", Timestamp.from(fromTime))
                .param("toTime", Timestamp.from(toTime))
                .param("status", status.name())
                .query(BigDecimal.class)
                .single();
        return total == null ? BigDecimal.ZERO : total;
    }

    private static final class TransactionRowMapper implements RowMapper<Transaction> {

        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transaction transaction = new Transaction();
            transaction.setId(UUID.fromString(rs.getString("id")));
            transaction.setAccountId(rs.getString("account_id"));
            transaction.setPayeeId(rs.getString("payee_id"));
            transaction.setAmount(rs.getBigDecimal("amount"));
            transaction.setCurrency(rs.getString("currency"));
            transaction.setType(TransactionType.valueOf(rs.getString("type")));
            transaction.setStatus(TransactionStatus.valueOf(rs.getString("status")));
            transaction.setTransactionTime(getInstant(rs, "transaction_time"));
            transaction.setDescription(rs.getString("description"));
            transaction.setCreatedAt(getInstant(rs, "created_at"));
            transaction.setUpdatedAt(getInstant(rs, "updated_at"));
            return transaction;
        }

        private Instant getInstant(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}

