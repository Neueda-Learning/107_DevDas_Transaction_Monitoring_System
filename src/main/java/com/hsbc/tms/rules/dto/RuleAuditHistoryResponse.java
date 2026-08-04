package com.hsbc.tms.rules.dto;

import com.hsbc.tms.rules.model.RuleAuditAction;
import java.time.Instant;

public record RuleAuditHistoryResponse(
        Long id,
        Long ruleId,
        RuleAuditAction action,
        String previousValues,
        String newValues,
        Instant changedAt,
        String changedBy) {
}

