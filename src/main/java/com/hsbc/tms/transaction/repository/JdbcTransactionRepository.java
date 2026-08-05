package com.hsbc.tms.transaction.repository;

import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import jakarta.annotation.Nullable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.DataIntegrityViolationException;
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

    // Bug fix 1: defined as a List to avoid trailing-newline artifacts from text-block split
    private static final List<String> REQUIRED_SELECT_COLUMNS = List.of(
            "id", "account_id", "payee_id", "amount", "currency", "type", "status",
            "transaction_time", "description", "created_at", "updated_at");

    private static final List<String> OPTIONAL_TRANSACTION_COLUMNS = List.of(
            "reviewed_by",
            "reviewed_at",
            "review_note",
            "rollback_reason_code",
            "rollback_reason_detail",
            "rollback_requested_by",
            "rollback_requested_at",
            "rollback_supporting_reference",
            "rollback_reviewed_by",
            "rollback_reviewed_at",
            "rollback_review_note",
            "refunded_at",
            "refund_transaction_id",
            "refunded_for_transaction_id");

    private final JdbcClient jdbcClient;
    private final DataSource dataSource;
    private volatile Set<String> availableTransactionColumns;

    public JdbcTransactionRepository(JdbcClient jdbcClient, DataSource dataSource) {
        this.jdbcClient = jdbcClient;
        this.dataSource = dataSource;
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

        // Bug fix 4: wrap duplicate-key violation with a meaningful message
        try {
            jdbcClient.sql(sql).params(params).update();
        } catch (DataIntegrityViolationException ex) {
            throw new DataIntegrityViolationException(
                    "Transaction with id " + transaction.getId() + " already exists", ex);
        }
        return transaction;
    }

    @Override
    public Transaction update(Transaction transaction) {
        Set<String> columns = getAvailableTransactionColumns();
        List<String> assignments = new ArrayList<>(List.of(
                "account_id = :accountId",
                "payee_id = :payeeId",
                "amount = :amount",
                "currency = :currency",
                "type = :type",
                "status = :status",
                "transaction_time = :transactionTime",
                "description = :description",
                "updated_at = :updatedAt"));

        addIfColumnExists(assignments, columns, "reviewed_by", "reviewed_by = :reviewedBy");
        addIfColumnExists(assignments, columns, "reviewed_at", "reviewed_at = :reviewedAt");
        addIfColumnExists(assignments, columns, "review_note", "review_note = :reviewNote");
        addIfColumnExists(assignments, columns, "rollback_reason_code", "rollback_reason_code = :rollbackReasonCode");
        addIfColumnExists(assignments, columns, "rollback_reason_detail", "rollback_reason_detail = :rollbackReasonDetail");
        addIfColumnExists(assignments, columns, "rollback_requested_by", "rollback_requested_by = :rollbackRequestedBy");
        addIfColumnExists(assignments, columns, "rollback_requested_at", "rollback_requested_at = :rollbackRequestedAt");
        addIfColumnExists(assignments, columns, "rollback_supporting_reference", "rollback_supporting_reference = :rollbackSupportingReference");
        addIfColumnExists(assignments, columns, "rollback_reviewed_by", "rollback_reviewed_by = :rollbackReviewedBy");
        addIfColumnExists(assignments, columns, "rollback_reviewed_at", "rollback_reviewed_at = :rollbackReviewedAt");
        addIfColumnExists(assignments, columns, "rollback_review_note", "rollback_review_note = :rollbackReviewNote");
        addIfColumnExists(assignments, columns, "refunded_at", "refunded_at = :refundedAt");
        addIfColumnExists(assignments, columns, "refund_transaction_id", "refund_transaction_id = :refundTransactionId");
        addIfColumnExists(assignments, columns, "refunded_for_transaction_id", "refunded_for_transaction_id = :refundedForTransactionId");

        String sql = "UPDATE transactions SET " + String.join(", ", assignments) + " WHERE id = :id";

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
                """.formatted(buildSelectColumns());

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
        // Bug fix 3: cap page size to prevent full-table fetches
        int pageSize = Math.min(pageable.getPageSize(), 200);
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

        String dataSql = "SELECT " + buildSelectColumns() + " FROM transactions"
                + whereClause + " ORDER BY " + buildOrderBy(pageable.getSort()) + " LIMIT :limit OFFSET :offset";

        params.put("limit", pageSize);
        params.put("offset", pageable.getOffset());

        List<Transaction> content = jdbcClient.sql(dataSql).params(params).query(TRANSACTION_ROW_MAPPER).list();
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildSelectColumns() {
        // Bug fix 1: REQUIRED_SELECT_COLUMNS is now a List — no split/trim needed
        List<String> columns = new ArrayList<>(REQUIRED_SELECT_COLUMNS);
        for (String optionalColumn : OPTIONAL_TRANSACTION_COLUMNS) {
            if (getAvailableTransactionColumns().contains(optionalColumn)) {
                columns.add(optionalColumn);
            }
        }
        return String.join(", ", columns);
    }

    private void addIfColumnExists(List<String> assignments, Set<String> columns, String columnName, String assignment) {
        if (columns.contains(columnName)) {
            assignments.add(assignment);
        }
    }

    private Set<String> getAvailableTransactionColumns() {
        if (availableTransactionColumns == null) {
            synchronized (this) {
                if (availableTransactionColumns == null) {
                    availableTransactionColumns = Collections.unmodifiableSet(loadTransactionColumns());
                }
            }
        }
        return availableTransactionColumns;
    }

    private Set<String> loadTransactionColumns() {
        Set<String> columns = new HashSet<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            collectColumns(metaData, connection.getCatalog(), null, "transactions", columns);
            if (columns.isEmpty()) {
                collectColumns(metaData, connection.getCatalog(), null, "TRANSACTIONS", columns);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect transactions table columns", ex);
        }
        return columns;
    }

    private void collectColumns(DatabaseMetaData metaData, @Nullable String catalog, @Nullable String schema, String tableName, Set<String> columns)
            throws SQLException {
        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }
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
            // Bug fix 2: build the column-presence Set ONCE per row — O(cols),
            // instead of scanning metadata for every optional field — O(cols × fields).
            Set<String> cols = buildColumnSet(rs);

            Transaction transaction = new Transaction();
            transaction.setId(UUID.fromString(rs.getString("id")));
            transaction.setAccountId(rs.getString("account_id"));
            transaction.setPayeeId(rs.getString("payee_id"));
            transaction.setAmount(rs.getBigDecimal("amount"));
            transaction.setCurrency(rs.getString("currency"));
            transaction.setType(TransactionType.valueOf(rs.getString("type")));
            transaction.setStatus(TransactionStatus.valueOf(rs.getString("status")));
            transaction.setTransactionTime(getInstant(rs, cols, "transaction_time"));
            transaction.setDescription(rs.getString("description"));
            transaction.setCreatedAt(getInstant(rs, cols, "created_at"));
            transaction.setUpdatedAt(getInstant(rs, cols, "updated_at"));
            transaction.setReviewedBy(getString(rs, cols, "reviewed_by"));
            transaction.setReviewedAt(getInstant(rs, cols, "reviewed_at"));
            transaction.setReviewNote(getString(rs, cols, "review_note"));
            transaction.setRollbackReasonCode(getString(rs, cols, "rollback_reason_code"));
            transaction.setRollbackReasonDetail(getString(rs, cols, "rollback_reason_detail"));
            transaction.setRollbackRequestedBy(getString(rs, cols, "rollback_requested_by"));
            transaction.setRollbackRequestedAt(getInstant(rs, cols, "rollback_requested_at"));
            transaction.setRollbackSupportingReference(getString(rs, cols, "rollback_supporting_reference"));
            transaction.setRollbackReviewedBy(getString(rs, cols, "rollback_reviewed_by"));
            transaction.setRollbackReviewedAt(getInstant(rs, cols, "rollback_reviewed_at"));
            transaction.setRollbackReviewNote(getString(rs, cols, "rollback_review_note"));
            transaction.setRefundedAt(getInstant(rs, cols, "refunded_at"));
            transaction.setRefundTransactionId(getUuid(rs, cols, "refund_transaction_id"));
            transaction.setRefundedForTransactionId(getUuid(rs, cols, "refunded_for_transaction_id"));
            return transaction;
        }

        /** Builds a lower-case set of column labels present in the ResultSet — called once per row. */
        private Set<String> buildColumnSet(ResultSet rs) throws SQLException {
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            Set<String> set = new HashSet<>(count * 2);
            for (int i = 1; i <= count; i++) {
                set.add(meta.getColumnLabel(i).toLowerCase());
            }
            return set;
        }

        private Instant getInstant(ResultSet rs, Set<String> cols, String column) throws SQLException {
            if (!cols.contains(column)) return null;
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : timestamp.toInstant();
        }

        private UUID getUuid(ResultSet rs, Set<String> cols, String column) throws SQLException {
            if (!cols.contains(column)) return null;
            String value = rs.getString(column);
            return value == null ? null : UUID.fromString(value);
        }

        private String getString(ResultSet rs, Set<String> cols, String column) throws SQLException {
            if (!cols.contains(column)) return null;
            return rs.getString(column);
        }
    }
}




