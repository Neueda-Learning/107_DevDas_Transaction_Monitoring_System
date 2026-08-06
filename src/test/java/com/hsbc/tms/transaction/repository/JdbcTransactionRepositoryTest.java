package com.hsbc.tms.transaction.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hsbc.tms.transaction.dto.TransactionFilterRequest;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcTransactionRepository.class)
@ActiveProfiles("test")
class JdbcTransactionRepositoryTest {

    @Autowired
    private JdbcTransactionRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM transactions").update();
    }

    @Test
    void saveAndFindById_roundTripsCoreFields() {
        Transaction tx = buildTransaction(
                UUID.randomUUID(),
                "ACC-100",
                "PAYEE-100",
                new BigDecimal("250.00"),
                TransactionStatus.COMPLETED,
                Instant.parse("2026-08-06T10:00:00Z"));

        repository.save(tx);

        Transaction found = repository.findById(tx.getId()).orElseThrow();
        assertThat(found.getAccountId()).isEqualTo("ACC-100");
        assertThat(found.getPayeeId()).isEqualTo("PAYEE-100");
        assertThat(found.getAmount()).isEqualByComparingTo("250.00");
        assertThat(found.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void save_throwsMeaningfulErrorForDuplicateId() {
        UUID id = UUID.randomUUID();
        Transaction first = buildTransaction(
                id,
                "ACC-1",
                "PAYEE-1",
                new BigDecimal("20.00"),
                TransactionStatus.PENDING,
                Instant.parse("2026-08-06T09:00:00Z"));
        Transaction duplicate = buildTransaction(
                id,
                "ACC-2",
                "PAYEE-2",
                new BigDecimal("30.00"),
                TransactionStatus.PENDING,
                Instant.parse("2026-08-06T10:00:00Z"));

        repository.save(first);

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void update_persistsOptionalDecisionAndRollbackColumns() {
        UUID id = UUID.randomUUID();
        Transaction tx = buildTransaction(
                id,
                "ACC-7",
                "PAYEE-7",
                new BigDecimal("400.00"),
                TransactionStatus.ROLLBACK_REQUESTED,
                Instant.parse("2026-08-06T08:00:00Z"));
        repository.save(tx);

        tx.setReviewedBy("operator-a");
        tx.setReviewedAt(Instant.parse("2026-08-06T08:05:00Z"));
        tx.setReviewNote("needs review");
        tx.setRollbackReasonCode("DUPLICATE");
        tx.setRollbackReasonDetail("duplicate transfer");
        tx.setRollbackRequestedBy("user-a");
        tx.setRollbackRequestedAt(Instant.parse("2026-08-06T08:06:00Z"));
        tx.setRollbackSupportingReference("CASE-1");
        tx.setRollbackReviewedBy("operator-b");
        tx.setRollbackReviewedAt(Instant.parse("2026-08-06T08:07:00Z"));
        tx.setRollbackReviewNote("approved");
        tx.setRefundedAt(Instant.parse("2026-08-06T08:08:00Z"));
        tx.setRefundTransactionId(UUID.randomUUID());
        tx.setRefundedForTransactionId(UUID.randomUUID());
        tx.setUpdatedAt(Instant.parse("2026-08-06T08:09:00Z"));

        repository.update(tx);

        Transaction found = repository.findById(id).orElseThrow();
        assertThat(found.getReviewedBy()).isEqualTo("operator-a");
        assertThat(found.getReviewNote()).isEqualTo("needs review");
        assertThat(found.getRollbackReasonCode()).isEqualTo("DUPLICATE");
        assertThat(found.getRollbackReasonDetail()).isEqualTo("duplicate transfer");
        assertThat(found.getRollbackRequestedBy()).isEqualTo("user-a");
        assertThat(found.getRollbackSupportingReference()).isEqualTo("CASE-1");
        assertThat(found.getRollbackReviewedBy()).isEqualTo("operator-b");
        assertThat(found.getRollbackReviewNote()).isEqualTo("approved");
        assertThat(found.getRefundTransactionId()).isEqualTo(tx.getRefundTransactionId());
        assertThat(found.getRefundedForTransactionId()).isEqualTo(tx.getRefundedForTransactionId());
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByFilter_appliesRangeFilterAndSortOrder() {
        repository.save(buildTransaction(
                UUID.randomUUID(),
                "ACC-FILTER",
                "PAYEE-1",
                new BigDecimal("500.00"),
                TransactionStatus.COMPLETED,
                Instant.parse("2026-08-06T06:00:00Z")));
        repository.save(buildTransaction(
                UUID.randomUUID(),
                "ACC-FILTER",
                "PAYEE-2",
                new BigDecimal("100.00"),
                TransactionStatus.COMPLETED,
                Instant.parse("2026-08-06T07:00:00Z")));
        repository.save(buildTransaction(
                UUID.randomUUID(),
                "ACC-OTHER",
                "PAYEE-9",
                new BigDecimal("999.00"),
                TransactionStatus.COMPLETED,
                Instant.parse("2026-08-06T08:00:00Z")));

        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setAccountId("ACC-FILTER");
        filter.setMinAmount(new BigDecimal("100.00"));
        filter.setMaxAmount(new BigDecimal("600.00"));
        filter.setFromTime(Instant.parse("2026-08-06T05:00:00Z"));
        filter.setToTime(Instant.parse("2026-08-06T09:00:00Z"));

        Page<Transaction> page = repository.findByFilter(
                filter,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "amount")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getAmount()).isEqualByComparingTo("100.00");
        assertThat(page.getContent().get(1).getAmount()).isEqualByComparingTo("500.00");
    }

    private Transaction buildTransaction(
            UUID id,
            String accountId,
            String payeeId,
            BigDecimal amount,
            TransactionStatus status,
            Instant transactionTime) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setAccountId(accountId);
        tx.setPayeeId(payeeId);
        tx.setAmount(amount);
        tx.setCurrency("USD");
        tx.setType(TransactionType.DEBIT);
        tx.setStatus(status);
        tx.setTransactionTime(transactionTime);
        tx.setDescription("seed");
        tx.setCreatedAt(transactionTime.minusSeconds(30));
        tx.setUpdatedAt(transactionTime.minusSeconds(10));
        return tx;
    }
}

