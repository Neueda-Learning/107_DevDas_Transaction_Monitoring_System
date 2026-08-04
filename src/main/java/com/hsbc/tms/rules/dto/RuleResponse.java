package com.hsbc.tms.rules.dto;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.math.BigDecimal;

public record RuleResponse(
        Long id,
        String name,
        RuleType type,
        AlertSeverity severity,
        boolean active,
        BigDecimal amountThreshold,
        Integer transactionCountThreshold,
        Integer timeWindowMinutes) {
}

