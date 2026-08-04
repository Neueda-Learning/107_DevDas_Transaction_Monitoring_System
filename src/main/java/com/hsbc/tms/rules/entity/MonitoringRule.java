package com.hsbc.tms.rules.entity;

import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.math.BigDecimal;
import java.time.Instant;

public class MonitoringRule {

    private Long id;
    private String name;
    private RuleType type;
    private AlertSeverity severity;
    private boolean active;
    private BigDecimal amountThreshold;
    private Integer transactionCountThreshold;
    private Integer timeWindowMinutes;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BigDecimal getAmountThreshold() {
        return amountThreshold;
    }

    public void setAmountThreshold(BigDecimal amountThreshold) {
        this.amountThreshold = amountThreshold;
    }

    public Integer getTransactionCountThreshold() {
        return transactionCountThreshold;
    }

    public void setTransactionCountThreshold(Integer transactionCountThreshold) {
        this.transactionCountThreshold = transactionCountThreshold;
    }

    public Integer getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

