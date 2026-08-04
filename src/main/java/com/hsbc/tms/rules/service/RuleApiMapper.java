package com.hsbc.tms.rules.service;

import com.hsbc.tms.rules.dto.RuleAuditHistoryResponse;
import com.hsbc.tms.rules.dto.RuleResponse;
import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.entity.RuleAuditHistory;

public final class RuleApiMapper {

    private RuleApiMapper() {
    }

    public static RuleResponse toRuleResponse(MonitoringRule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getType(),
                rule.getSeverity(),
                rule.isActive(),
                rule.getAmountThreshold(),
                rule.getTransactionCountThreshold(),
                rule.getTimeWindowMinutes());
    }

    public static RuleAuditHistoryResponse toRuleAuditHistoryResponse(RuleAuditHistory history) {
        return new RuleAuditHistoryResponse(
                history.getId(),
                history.getRuleId(),
                history.getAction(),
                history.getPreviousValues(),
                history.getNewValues(),
                history.getChangedAt(),
                history.getChangedBy());
    }
}

