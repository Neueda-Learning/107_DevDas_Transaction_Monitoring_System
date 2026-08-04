package com.hsbc.tms.rules.entity;

import com.hsbc.tms.rules.model.RuleAuditAction;
import java.time.Instant;

public class RuleAuditHistory {

    private Long id;
    private Long ruleId;
    private RuleAuditAction action;
    private String previousValues;
    private String newValues;
    private Instant changedAt;
    private String changedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public RuleAuditAction getAction() {
        return action;
    }

    public void setAction(RuleAuditAction action) {
        this.action = action;
    }

    public String getPreviousValues() {
        return previousValues;
    }

    public void setPreviousValues(String previousValues) {
        this.previousValues = previousValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}

