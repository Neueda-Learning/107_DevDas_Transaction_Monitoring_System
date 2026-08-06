package com.hsbc.tms.transaction.dto;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;

public class AlertReferenceResponse {
    private Long id;
    private String ruleName;
    private RuleType ruleType;
    private AlertSeverity severity;
    private String message;

    public AlertReferenceResponse() {}

    public AlertReferenceResponse(Long id, String ruleName, RuleType ruleType, AlertSeverity severity, String message) {
        this.id = id;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.severity = severity;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

