package com.hsbc.tms.alerts.entity;

import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Alert {

    private Long id;
    private String ruleName;
    private RuleType ruleType;
    private AlertSeverity severity;
    private AlertStatus status;
    private String message;
    private Instant createdAt;
    private Instant updatedAt;
    private List<UUID> triggeringTransactionIds = new ArrayList<>();

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

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<UUID> getTriggeringTransactionIds() {
        return triggeringTransactionIds;
    }

    public void setTriggeringTransactionIds(List<UUID> triggeringTransactionIds) {
        this.triggeringTransactionIds = triggeringTransactionIds;
    }
}


