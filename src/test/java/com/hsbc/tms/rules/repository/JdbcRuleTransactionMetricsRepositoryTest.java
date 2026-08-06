package com.hsbc.tms.rules.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
@Import(JdbcRuleTransactionMetricsRepository.class)
@ActiveProfiles("test")
class JdbcRuleTransactionMetricsRepositoryTest {

    @Autowired
    private JdbcRuleTransactionMetricsRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM transactions").update();
    }

    @Test
    void countByAccountIdAndTransactionTimeBetween_returnsExpectedCount() {
        insertTransaction("A-1", "P-1", "100.00", Instant.parse("2026-08-05T10:00:00Z"));
        insertTransaction("A-1", "P-2", "200.00", Instant.parse("2026-08-05T11:00:00Z"));
        insertTransaction("A-2", "P-1", "300.00", Instant.parse("2026-08-05T11:30:00Z"));

        long count = repository.countByAccountIdAndTransactionTimeBetween(
                "A-1",
                Instant.parse("2026-08-05T09:00:00Z"),
                Instant.parse("2026-08-05T11:30:00Z"));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByAccountIdAndTransactionTimeBetween_returnsMappedRowsInOrder() {
        UUID older = insertTransaction("A-3", "P-1", "50.00", Instant.parse("2026-08-05T10:00:00Z"));
        UUID newer = insertTransaction("A-3", "P-2", "60.00", Instant.parse("2026-08-05T10:10:00Z"));

        List<Transaction> rows = repository.findByAccountIdAndTransactionTimeBetween(
                "A-3",
                Instant.parse("2026-08-05T09:55:00Z"),
                Instant.parse("2026-08-05T10:15:00Z"));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getId()).isEqualTo(older);
        assertThat(rows.get(1).getId()).isEqualTo(newer);
        assertThat(rows.get(0).getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(rows.get(0).getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void countByAccountIdAndPayeeIdAndTransactionTimeBefore_returnsExpectedCount() {
        insertTransaction("A-7", "P-9", "20.00", Instant.parse("2026-08-05T08:00:00Z"));
        insertTransaction("A-7", "P-9", "30.00", Instant.parse("2026-08-05T09:00:00Z"));
        insertTransaction("A-7", "P-8", "40.00", Instant.parse("2026-08-05T09:30:00Z"));

        long count = repository.countByAccountIdAndPayeeIdAndTransactionTimeBefore(
                "A-7",
                "P-9",
                Instant.parse("2026-08-05T09:30:00Z"));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void sumAmountByAccountAndTransactionTimeRange_returnsTotalOrZero() {
        insertTransaction("A-10", "P-1", "75.25", Instant.parse("2026-08-05T10:00:00Z"));
        insertTransaction("A-10", "P-2", "24.75", Instant.parse("2026-08-05T11:00:00Z"));

        BigDecimal total = repository.sumAmountByAccountAndTransactionTimeRange(
                "A-10",
                Instant.parse("2026-08-05T09:00:00Z"),
                Instant.parse("2026-08-05T12:00:00Z"));

        BigDecimal zero = repository.sumAmountByAccountAndTransactionTimeRange(
                "A-404",
                Instant.parse("2026-08-05T09:00:00Z"),
                Instant.parse("2026-08-05T12:00:00Z"));

        assertThat(total).isEqualByComparingTo("100.00");
        assertThat(zero).isEqualByComparingTo("0");
    }

    private UUID insertTransaction(String accountId, String payeeId, String amount, Instant transactionTime) {
        UUID id = UUID.randomUUID();
        Instant createdAt = transactionTime.minusSeconds(60);
        Instant updatedAt = transactionTime.minusSeconds(30);

        String sql = """
                INSERT INTO transactions (
                    id, account_id, payee_id, amount, currency, type, status,
                    transaction_time, description, created_at, updated_at
                ) VALUES (
                    :id, :accountId, :payeeId, :amount, :currency, :type, :status,
                    :transactionTime, :description, :createdAt, :updatedAt
                )
                """;

        jdbcClient.sql(sql)
                .param("id", id.toString())
                .param("accountId", accountId)
                .param("payeeId", payeeId)
                .param("amount", new BigDecimal(amount))
                .param("currency", "USD")
                .param("type", TransactionType.DEBIT.name())
                .param("status", TransactionStatus.COMPLETED.name())
                .param("transactionTime", Timestamp.from(transactionTime))
                .param("description", "test")
                .param("createdAt", Timestamp.from(createdAt))
                .param("updatedAt", Timestamp.from(updatedAt))
                .update();

        return id;
    }
}

