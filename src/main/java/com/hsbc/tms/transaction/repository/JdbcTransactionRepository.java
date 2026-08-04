package com.hsbc.tms.transaction.repository;

import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransactionRepository implements TransactionRepository {

    private static final RowMapper<Transaction> TRANSACTION_ROW_MAPPER = new TransactionRowMapper();

    private static final String SELECT_COLUMNS = """
            id, account_id, payee_id, amount, currency, type, status,
            transaction_time, description, created_at, updated_at,
            reviewed_by, reviewed_at, review_note,
            rollback_reason_code, rollback_reason_detail, rollback_requested_by, rollback_requested_at,
            rollback_supporting_reference, rollback_reviewed_by, rollback_reviewed_at, rollback_review_note,
            refunded_at, refund_transaction_id, refunded_for_transaction_id
            """;

    private final JdbcClient jdbcClient;

    public JdbcTransactionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Transaction save(Transaction transaction) {
        String sql = """
                INSERT INTO transactions (
                    id, account_id, payee_id, amount, currency, type, status,
                    transaction_time, description, created_at, updated_at
                ) VALUES (
                    :id, :accountId, :payeeId, :amount, :currency, :type, :status,
                    :transactionTime, :description, :createdAt, :updatedAt
                )
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", transaction.getId().toString());
        params.put("accountId", transaction.getAccountId());
        params.put("payeeId", transaction.getPayeeId());
        params.put("amount", transaction.getAmount());
        params.put("currency", transaction.getCurrency());
        params.put("type", transaction.getType().name());
        params.put("status", transaction.getStatus().name());
        params.put("transactionTime", Timestamp.from(transaction.getTransactionTime()));
        params.put("description", transaction.getDescription());
        params.put("createdAt", Timestamp.from(transaction.getCreatedAt()));
        params.put("updatedAt", Timestamp.from(transaction.getUpdatedAt()));

        jdbcClient.sql(sql).params(params).update();
        return transaction;
    }

    @Override
    public Transaction update(Transaction transaction) {
        String sql = """
                UPDATE transactions SET
                    account_id = :accountId, payee_id = :payeeId, amount = :amount, currency = :currency,
                    type = :type, status = :status, transaction_time = :transactionTime,
                    description = :description, updated_at = :updatedAt,
                    reviewed_by = :reviewedBy, reviewed_at = :reviewedAt, review_note = :reviewNote,
                    rollback_reason_code = :rollbackReasonCode, rollback_reason_detail = :rollbackReasonDetail,
                    rollback_requested_by = :rollbackRequestedBy, rollback_requested_at = :rollbackRequestedAt,
                    rollback_supporting_reference = :rollbackSupportingReference,
                    rollback_reviewed_by = :rollbackReviewedBy, rollback_reviewed_at = :rollbackReviewedAt,
                    rollback_review_note = :rollbackReviewNote,
                    refunded_at = :refundedAt, refund_transaction_id = :refundTransactionId,
                    refunded_for_transaction_id = :refundedForTransactionId
                WHERE id = :id
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", transaction.getId().toString());
        params.put("accountId", transaction.getAccountId());
        params.put("payeeId", transaction.getPayeeId());
        params.put("amount", transaction.getAmount());
        params.put("currency", transaction.getCurrency());
        params.put("type", transaction.getType().name());
        params.put("status", transaction.getStatus().name());
        params.put("transactionTime", Timestamp.from(transaction.getTransactionTime()));
        params.put("description", transaction.getDescription());
        params.put("updatedAt", Timestamp.from(transaction.getUpdatedAt()));
        params.put("reviewedBy", transaction.getReviewedBy());
        params.put("reviewedAt", toTimestamp(transaction.getReviewedAt()));
        params.put("reviewNote", transaction.getReviewNote());
        params.put("rollbackReasonCode", transaction.getRollbackReasonCode());
        params.put("rollbackReasonDetail", transaction.getRollbackReasonDetail());
        params.put("rollbackRequestedBy", transaction.getRollbackRequestedBy());
        params.put("rollbackRequestedAt", toTimestamp(transaction.getRollbackRequestedAt()));
        params.put("rollbackSupportingReference", transaction.getRollbackSupportingReference());
        params.put("rollbackReviewedBy", transaction.getRollbackReviewedBy());
        params.put("rollbackReviewedAt", toTimestamp(transaction.getRollbackReviewedAt()));
        params.put("rollbackReviewNote", transaction.getRollbackReviewNote());
        params.put("refundedAt", toTimestamp(transaction.getRefundedAt()));
        params.put("refundTransactionId", transaction.getRefundTransactionId() == null ? null : transaction.getRefundTransactionId().toString());
        params.put("refundedForTransactionId", transaction.getRefundedForTransactionId() == null ? null : transaction.getRefundedForTransactionId().toString());

        jdbcClient.sql(sql).params(params).update();
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        String sql = """
                SELECT %s
                FROM transactions
                WHERE id = :id
                """.formatted(SELECT_COLUMNS);

        try {
            Transaction transaction = jdbcClient.sql(sql)
                    .param("id", id.toString())
                    .query(TRANSACTION_ROW_MAPPER)
                    .single();
            return Optional.ofNullable(transaction);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Page<Transaction> findByFilter(TransactionFilterRequest filter, Pageable pageable) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (hasText(filter.getAccountId())) {
            whereClause.append(" AND account_id = :accountId ");
            params.put("accountId", filter.getAccountId());
        }
        if (hasText(filter.getPayeeId())) {
            whereClause.append(" AND payee_id = :payeeId ");
            params.put("payeeId", filter.getPayeeId());
        }
        if (filter.getStatus() != null) {
            whereClause.append(" AND status = :status ");
            params.put("status", filter.getStatus().name());
        }
        if (filter.getType() != null) {
            whereClause.append(" AND type = :type ");
            params.put("type", filter.getType().name());
        }
        if (filter.getMinAmount() != null) {
            whereClause.append(" AND amount >= :minAmount ");
            params.put("minAmount", filter.getMinAmount());
        }
        if (filter.getMaxAmount() != null) {
            whereClause.append(" AND amount <= :maxAmount ");
            params.put("maxAmount", filter.getMaxAmount());
        }
        if (filter.getFromTime() != null) {
            whereClause.append(" AND transaction_time >= :fromTime ");
            params.put("fromTime", Timestamp.from(filter.getFromTime()));
        }
        if (filter.getToTime() != null) {
            whereClause.append(" AND transaction_time <= :toTime ");
            params.put("toTime", Timestamp.from(filter.getToTime()));
        }

        String countSql = "SELECT COUNT(*) FROM transactions" + whereClause;
        Long total = jdbcClient.sql(countSql).params(params).query(Long.class).single();

        String dataSql = "SELECT " + SELECT_COLUMNS + " FROM transactions"
                + whereClause + " ORDER BY " + buildOrderBy(pageable.getSort()) + " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Transaction> content = jdbcClient.sql(dataSql).params(params).query(TRANSACTION_ROW_MAPPER).list();
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private String buildOrderBy(Sort sort) {
        Sort.Order order = sort.stream().findFirst().orElse(Sort.Order.desc("transactionTime"));
        String column = toColumnName(order.getProperty());
        String direction = order.getDirection().isAscending() ? "ASC" : "DESC";
        return column + " " + direction;
    }

    private String toColumnName(String property) {
        return switch (property) {
            case "amount" -> "amount";
            case "transactionTime" -> "transaction_time";
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "accountId" -> "account_id";
            case "status" -> "status";
            default -> "transaction_time";
        };
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
            transaction.setReviewedBy(rs.getString("reviewed_by"));
            transaction.setReviewedAt(getInstant(rs, "reviewed_at"));
            transaction.setReviewNote(rs.getString("review_note"));
            transaction.setRollbackReasonCode(rs.getString("rollback_reason_code"));
            transaction.setRollbackReasonDetail(rs.getString("rollback_reason_detail"));
            transaction.setRollbackRequestedBy(rs.getString("rollback_requested_by"));
            transaction.setRollbackRequestedAt(getInstant(rs, "rollback_requested_at"));
            transaction.setRollbackSupportingReference(rs.getString("rollback_supporting_reference"));
            transaction.setRollbackReviewedBy(rs.getString("rollback_reviewed_by"));
            transaction.setRollbackReviewedAt(getInstant(rs, "rollback_reviewed_at"));
            transaction.setRollbackReviewNote(rs.getString("rollback_review_note"));
            transaction.setRefundedAt(getInstant(rs, "refunded_at"));
            transaction.setRefundTransactionId(getUuid(rs, "refund_transaction_id"));
            transaction.setRefundedForTransactionId(getUuid(rs, "refunded_for_transaction_id"));
            return transaction;
        }

        private Instant getInstant(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : timestamp.toInstant();
        }

        private UUID getUuid(ResultSet rs, String column) throws SQLException {
            String value = rs.getString(column);
            return value == null ? null : UUID.fromString(value);
        }
    }
}




