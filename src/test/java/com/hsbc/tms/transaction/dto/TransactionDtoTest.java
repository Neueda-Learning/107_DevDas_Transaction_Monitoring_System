package com.hsbc.tms.transaction.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionDtoTest {

    @Test
    void createTransactionRequest_exposesAllValues() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId("ACC-1");
        request.setPayeeId("PAYEE-1");
        request.setAmount(new BigDecimal("12.34"));
        request.setCurrency("USD");
        request.setType(TransactionType.CREDIT);
        request.setStatus(TransactionStatus.PENDING);
        request.setTransactionTime(Instant.parse("2026-08-06T10:00:00Z"));
        request.setDescription("salary");

        assertThat(request.getAccountId()).isEqualTo("ACC-1");
        assertThat(request.getPayeeId()).isEqualTo("PAYEE-1");
        assertThat(request.getAmount()).isEqualByComparingTo("12.34");
        assertThat(request.getCurrency()).isEqualTo("USD");
        assertThat(request.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(request.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(request.getTransactionTime()).isEqualTo(Instant.parse("2026-08-06T10:00:00Z"));
        assertThat(request.getDescription()).isEqualTo("salary");
    }

    @Test
    void rollbackAndDecisionRecords_exposeValues() {
        TransactionDecisionRequest decision = new TransactionDecisionRequest("op-1", "approved");
        TransactionRollbackRequest rollback = new TransactionRollbackRequest("DUP", "duplicate", "user-1", "CASE-9");
        TransactionRollbackDecisionRequest rollbackDecision = new TransactionRollbackDecisionRequest("op-2", "rejected");

        assertThat(decision.operatorId()).isEqualTo("op-1");
        assertThat(decision.note()).isEqualTo("approved");
        assertThat(rollback.reasonCode()).isEqualTo("DUP");
        assertThat(rollback.reasonDetail()).isEqualTo("duplicate");
        assertThat(rollback.requestedBy()).isEqualTo("user-1");
        assertThat(rollback.supportingReference()).isEqualTo("CASE-9");
        assertThat(rollbackDecision.operatorId()).isEqualTo("op-2");
        assertThat(rollbackDecision.note()).isEqualTo("rejected");
    }

    @Test
    void filterAndResponseDtos_exposeValues() {
        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setAccountId("ACC-2");
        filter.setPayeeId("PAYEE-2");
        filter.setStatus(TransactionStatus.COMPLETED);
        filter.setType(TransactionType.DEBIT);
        filter.setMinAmount(new BigDecimal("1.00"));
        filter.setMaxAmount(new BigDecimal("99.00"));
        filter.setFromTime(Instant.parse("2026-08-06T00:00:00Z"));
        filter.setToTime(Instant.parse("2026-08-06T23:59:59Z"));

        assertThat(filter.getAccountId()).isEqualTo("ACC-2");
        assertThat(filter.getPayeeId()).isEqualTo("PAYEE-2");
        assertThat(filter.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(filter.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(filter.getMinAmount()).isEqualByComparingTo("1.00");
        assertThat(filter.getMaxAmount()).isEqualByComparingTo("99.00");
        assertThat(filter.getFromTime()).isEqualTo(Instant.parse("2026-08-06T00:00:00Z"));
        assertThat(filter.getToTime()).isEqualTo(Instant.parse("2026-08-06T23:59:59Z"));

        UUID id = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        UUID refundedFor = UUID.randomUUID();
        TransactionResponse response = new TransactionResponse();
        response.setId(id);
        response.setAccountId("ACC-2");
        response.setPayeeId("PAYEE-2");
        response.setAmount(new BigDecimal("9.99"));
        response.setCurrency("EUR");
        response.setType(TransactionType.CREDIT);
        response.setStatus(TransactionStatus.REFUNDED);
        response.setTransactionTime(Instant.parse("2026-08-06T11:00:00Z"));
        response.setDescription("refund");
        response.setCreatedAt(Instant.parse("2026-08-06T10:00:00Z"));
        response.setUpdatedAt(Instant.parse("2026-08-06T12:00:00Z"));
        response.setReviewedBy("operator");
        response.setReviewedAt(Instant.parse("2026-08-06T12:01:00Z"));
        response.setReviewNote("ok");
        response.setRollbackReasonCode("DUP");
        response.setRollbackReasonDetail("duplicate");
        response.setRollbackRequestedBy("requester");
        response.setRollbackRequestedAt(Instant.parse("2026-08-06T12:02:00Z"));
        response.setRollbackSupportingReference("CASE");
        response.setRollbackReviewedBy("rev");
        response.setRollbackReviewedAt(Instant.parse("2026-08-06T12:03:00Z"));
        response.setRollbackReviewNote("done");
        response.setRefundedAt(Instant.parse("2026-08-06T12:04:00Z"));
        response.setRefundTransactionId(refundId);
        response.setRefundedForTransactionId(refundedFor);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getAccountId()).isEqualTo("ACC-2");
        assertThat(response.getPayeeId()).isEqualTo("PAYEE-2");
        assertThat(response.getAmount()).isEqualByComparingTo("9.99");
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(response.getTransactionTime()).isEqualTo(Instant.parse("2026-08-06T11:00:00Z"));
        assertThat(response.getDescription()).isEqualTo("refund");
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-08-06T10:00:00Z"));
        assertThat(response.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-06T12:00:00Z"));
        assertThat(response.getReviewedBy()).isEqualTo("operator");
        assertThat(response.getReviewedAt()).isEqualTo(Instant.parse("2026-08-06T12:01:00Z"));
        assertThat(response.getReviewNote()).isEqualTo("ok");
        assertThat(response.getRollbackReasonCode()).isEqualTo("DUP");
        assertThat(response.getRollbackReasonDetail()).isEqualTo("duplicate");
        assertThat(response.getRollbackRequestedBy()).isEqualTo("requester");
        assertThat(response.getRollbackRequestedAt()).isEqualTo(Instant.parse("2026-08-06T12:02:00Z"));
        assertThat(response.getRollbackSupportingReference()).isEqualTo("CASE");
        assertThat(response.getRollbackReviewedBy()).isEqualTo("rev");
        assertThat(response.getRollbackReviewedAt()).isEqualTo(Instant.parse("2026-08-06T12:03:00Z"));
        assertThat(response.getRollbackReviewNote()).isEqualTo("done");
        assertThat(response.getRefundedAt()).isEqualTo(Instant.parse("2026-08-06T12:04:00Z"));
        assertThat(response.getRefundTransactionId()).isEqualTo(refundId);
        assertThat(response.getRefundedForTransactionId()).isEqualTo(refundedFor);
    }
}

