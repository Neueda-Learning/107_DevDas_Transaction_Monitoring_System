package com.hsbc.tms.alerts.dto;

import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.transaction.dto.TransactionResponse;
import java.time.Instant;
import java.util.List;

public record AlertResponse(
        Long id,
        String ruleName,
        RuleType ruleType,
        AlertSeverity severity,
        AlertStatus status,
        String message,
        Instant createdAt,
        Instant updatedAt,
        List<TransactionResponse> triggeringTransactions,
        List<AlertHistoryResponse> history) {
}


