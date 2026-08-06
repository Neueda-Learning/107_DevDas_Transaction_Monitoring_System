package com.hsbc.tms.alerts.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.alerts.dto.AlertHistoryResponse;
import com.hsbc.tms.alerts.dto.AlertResponse;
import com.hsbc.tms.alerts.entity.Alert;
import com.hsbc.tms.alerts.entity.AlertHistory;
import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import com.hsbc.tms.transaction.model.Transaction;
import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertApiMapperTest {

    @Test
    void toAlertHistoryResponse_mapsAllFields() {
        Instant createdAt = Instant.parse("2026-08-06T10:10:00Z");
        AlertHistory history = new AlertHistory();
        history.setId(8L);
        history.setAlertId(3L);
        history.setFromStatus(AlertStatus.OPEN);
        history.setToStatus(AlertStatus.INVESTIGATING);
        history.setNote("Checking");
        history.setChangedBy("analyst-2");
        history.setCreatedAt(createdAt);

        AlertHistoryResponse response = AlertApiMapper.toAlertHistoryResponse(history);

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.fromStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(response.toStatus()).isEqualTo(AlertStatus.INVESTIGATING);
        assertThat(response.note()).isEqualTo("Checking");
        assertThat(response.changedBy()).isEqualTo("analyst-2");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toAlertResponse_mapsAllFields() {
        Instant createdAt = Instant.parse("2026-08-06T08:00:00Z");
        Alert alert = new Alert();
        alert.setId(2L);
        alert.setRuleName("Velocity");
        alert.setRuleType(RuleType.VELOCITY);
        alert.setSeverity(AlertSeverity.MEDIUM);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setMessage("Velocity exceeded");
        alert.setCreatedAt(createdAt);
        alert.setUpdatedAt(createdAt.plusSeconds(30));

        TransactionResponse txn = new TransactionResponse();
        txn.setId(UUID.randomUUID());

        AlertHistoryResponse history = new AlertHistoryResponse(
                1L,
                null,
                AlertStatus.OPEN,
                "Created",
                "system",
                createdAt);

        AlertResponse response = AlertApiMapper.toAlertResponse(alert, List.of(txn), List.of(history));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.ruleName()).isEqualTo("Velocity");
        assertThat(response.ruleType()).isEqualTo(RuleType.VELOCITY);
        assertThat(response.severity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(response.message()).isEqualTo("Velocity exceeded");
        assertThat(response.triggeringTransactions()).containsExactly(txn);
        assertThat(response.history()).containsExactly(history);
    }

    @Test
    void toTransactionResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-06T09:00:00Z");

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAccountId("ACC-1");
        transaction.setPayeeId("PAY-1");
        transaction.setAmount(new BigDecimal("199.99"));
        transaction.setCurrency("USD");
        transaction.setType(TransactionType.DEBIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionTime(now.minusSeconds(60));
        transaction.setDescription("desc");
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now.plusSeconds(5));

        TransactionResponse response = AlertApiMapper.toTransactionResponse(transaction);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getAccountId()).isEqualTo("ACC-1");
        assertThat(response.getPayeeId()).isEqualTo("PAY-1");
        assertThat(response.getAmount()).isEqualByComparingTo("199.99");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getTransactionTime()).isEqualTo(now.minusSeconds(60));
        assertThat(response.getDescription()).isEqualTo("desc");
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now.plusSeconds(5));
    }
}
