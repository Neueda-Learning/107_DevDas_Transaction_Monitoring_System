package com.hsbc.tms.rules.entity;

import com.hsbc.tms.rules.model.RuleExecutionOutcome;
import java.time.Instant;
import java.util.UUID;

public class RuleExecutionHistory {

    private Long id;
    private UUID executionId;
    private Long ruleId;
    private UUID transactionId;
    private RuleExecutionOutcome outcome;
    private String message;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public RuleExecutionOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(RuleExecutionOutcome outcome) {
        this.outcome = outcome;
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
}

