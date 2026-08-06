package com.hsbc.tms.alerts.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertDtoRecordTest {

    @Test
    void createAlertRequest_exposesAllValues() {
        UUID transactionId = UUID.randomUUID();

        CreateAlertRequest request = new CreateAlertRequest(
                "High Amount",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                "Threshold exceeded",
                "analyst-1",
                "created manually",
                List.of(transactionId));

        assertThat(request.ruleName()).isEqualTo("High Amount");
        assertThat(request.ruleType()).isEqualTo(RuleType.AMOUNT_THRESHOLD);
        assertThat(request.severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(request.message()).isEqualTo("Threshold exceeded");
        assertThat(request.operatorId()).isEqualTo("analyst-1");
        assertThat(request.note()).isEqualTo("created manually");
        assertThat(request.triggeringTransactionIds()).containsExactly(transactionId);
    }

    @Test
    void alertStatusUpdateRequest_exposesAllValues() {
        AlertStatusUpdateRequest request = new AlertStatusUpdateRequest(AlertStatus.CLOSED, "analyst-2", "closed");

        assertThat(request.status()).isEqualTo(AlertStatus.CLOSED);
        assertThat(request.operatorId()).isEqualTo("analyst-2");
        assertThat(request.note()).isEqualTo("closed");
    }

    @Test
    void alertResponse_exposesAllValues() {
        Instant createdAt = Instant.parse("2026-08-06T10:00:00Z");
        TransactionResponse transaction = new TransactionResponse();
        transaction.setId(UUID.randomUUID());
        AlertHistoryResponse history = new AlertHistoryResponse(4L, null, AlertStatus.OPEN, "created", "system", createdAt);

        AlertResponse response = new AlertResponse(
                12L,
                "New Payee",
                RuleType.NEW_PAYEE,
                AlertSeverity.MEDIUM,
                AlertStatus.ACKNOWLEDGED,
                "new payee flagged",
                createdAt,
                createdAt.plusSeconds(5),
                List.of(transaction),
                List.of(history));

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.ruleName()).isEqualTo("New Payee");
        assertThat(response.ruleType()).isEqualTo(RuleType.NEW_PAYEE);
        assertThat(response.severity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(response.message()).isEqualTo("new payee flagged");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(createdAt.plusSeconds(5));
        assertThat(response.triggeringTransactions()).containsExactly(transaction);
        assertThat(response.history()).containsExactly(history);
    }

    @Test
    void alertHistoryResponse_exposesAllValues() {
        Instant at = Instant.parse("2026-08-06T11:00:00Z");

        AlertHistoryResponse response = new AlertHistoryResponse(
                9L,
                AlertStatus.OPEN,
                AlertStatus.DISMISSED,
                "false positive",
                "analyst-7",
                at);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.fromStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(response.toStatus()).isEqualTo(AlertStatus.DISMISSED);
        assertThat(response.note()).isEqualTo("false positive");
        assertThat(response.changedBy()).isEqualTo("analyst-7");
        assertThat(response.createdAt()).isEqualTo(at);
    }
}

