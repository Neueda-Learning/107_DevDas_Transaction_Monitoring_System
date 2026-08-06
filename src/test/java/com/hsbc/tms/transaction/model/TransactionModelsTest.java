package com.hsbc.tms.transaction.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionModelsTest {

    @Test
    void transactionType_containsExpectedValues() {
        assertThat(TransactionType.values()).containsExactly(TransactionType.DEBIT, TransactionType.CREDIT);
        assertThat(TransactionType.valueOf("DEBIT")).isEqualTo(TransactionType.DEBIT);
    }

    @Test
    void transactionStatus_containsExpectedValues() {
        assertThat(TransactionStatus.values()).containsExactly(
                TransactionStatus.COMPLETED,
                TransactionStatus.PENDING,
                TransactionStatus.FAILED,
                TransactionStatus.PENDING_APPROVAL,
                TransactionStatus.REJECTED,
                TransactionStatus.ROLLBACK_REQUESTED,
                TransactionStatus.ROLLBACK_REJECTED,
                TransactionStatus.REFUNDED);
        assertThat(TransactionStatus.valueOf("REFUNDED")).isEqualTo(TransactionStatus.REFUNDED);
    }

    @Test
    void transaction_exposesAllProperties() {
        UUID id = UUID.randomUUID();
        UUID refundTransactionId = UUID.randomUUID();
        UUID refundedForTransactionId = UUID.randomUUID();

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAccountId("ACC-10");
        transaction.setPayeeId("PAYEE-10");
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setCurrency("USD");
        transaction.setType(TransactionType.DEBIT);
        transaction.setStatus(TransactionStatus.ROLLBACK_REQUESTED);
        transaction.setTransactionTime(Instant.parse("2026-08-06T12:00:00Z"));
        transaction.setDescription("description");
        transaction.setCreatedAt(Instant.parse("2026-08-06T11:00:00Z"));
        transaction.setUpdatedAt(Instant.parse("2026-08-06T12:30:00Z"));
        transaction.setReviewedBy("operator");
        transaction.setReviewedAt(Instant.parse("2026-08-06T12:31:00Z"));
        transaction.setReviewNote("review note");
        transaction.setRollbackReasonCode("DUP");
        transaction.setRollbackReasonDetail("reason");
        transaction.setRollbackRequestedBy("requester");
        transaction.setRollbackRequestedAt(Instant.parse("2026-08-06T12:32:00Z"));
        transaction.setRollbackSupportingReference("CASE-100");
        transaction.setRollbackReviewedBy("reviewer");
        transaction.setRollbackReviewedAt(Instant.parse("2026-08-06T12:33:00Z"));
        transaction.setRollbackReviewNote("rollback note");
        transaction.setRefundedAt(Instant.parse("2026-08-06T12:34:00Z"));
        transaction.setRefundTransactionId(refundTransactionId);
        transaction.setRefundedForTransactionId(refundedForTransactionId);

        assertThat(transaction.getId()).isEqualTo(id);
        assertThat(transaction.getAccountId()).isEqualTo("ACC-10");
        assertThat(transaction.getPayeeId()).isEqualTo("PAYEE-10");
        assertThat(transaction.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(transaction.getCurrency()).isEqualTo("USD");
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.ROLLBACK_REQUESTED);
        assertThat(transaction.getTransactionTime()).isEqualTo(Instant.parse("2026-08-06T12:00:00Z"));
        assertThat(transaction.getDescription()).isEqualTo("description");
        assertThat(transaction.getCreatedAt()).isEqualTo(Instant.parse("2026-08-06T11:00:00Z"));
        assertThat(transaction.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-06T12:30:00Z"));
        assertThat(transaction.getReviewedBy()).isEqualTo("operator");
        assertThat(transaction.getReviewedAt()).isEqualTo(Instant.parse("2026-08-06T12:31:00Z"));
        assertThat(transaction.getReviewNote()).isEqualTo("review note");
        assertThat(transaction.getRollbackReasonCode()).isEqualTo("DUP");
        assertThat(transaction.getRollbackReasonDetail()).isEqualTo("reason");
        assertThat(transaction.getRollbackRequestedBy()).isEqualTo("requester");
        assertThat(transaction.getRollbackRequestedAt()).isEqualTo(Instant.parse("2026-08-06T12:32:00Z"));
        assertThat(transaction.getRollbackSupportingReference()).isEqualTo("CASE-100");
        assertThat(transaction.getRollbackReviewedBy()).isEqualTo("reviewer");
        assertThat(transaction.getRollbackReviewedAt()).isEqualTo(Instant.parse("2026-08-06T12:33:00Z"));
        assertThat(transaction.getRollbackReviewNote()).isEqualTo("rollback note");
        assertThat(transaction.getRefundedAt()).isEqualTo(Instant.parse("2026-08-06T12:34:00Z"));
        assertThat(transaction.getRefundTransactionId()).isEqualTo(refundTransactionId);
        assertThat(transaction.getRefundedForTransactionId()).isEqualTo(refundedForTransactionId);
    }
}

