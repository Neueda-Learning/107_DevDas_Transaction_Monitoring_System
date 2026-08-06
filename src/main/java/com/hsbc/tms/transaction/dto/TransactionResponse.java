package com.hsbc.tms.transaction.dto;

import com.hsbc.tms.transaction.model.TransactionStatus;
import com.hsbc.tms.transaction.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TransactionResponse {

    private UUID id;
    private String accountId;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private Instant transactionTime;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    private String reviewedBy;
    private Instant reviewedAt;
    private String reviewNote;

    private String rollbackReasonCode;
    private String rollbackReasonDetail;
    private String rollbackRequestedBy;
    private Instant rollbackRequestedAt;
    private String rollbackSupportingReference;
    private String rollbackReviewedBy;
    private Instant rollbackReviewedAt;
    private String rollbackReviewNote;

    private Instant refundedAt;
    private UUID refundTransactionId;
    private UUID refundedForTransactionId;

    private List<AlertReferenceResponse> triggeredAlerts;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(String payeeId) {
        this.payeeId = payeeId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Instant transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getRollbackReasonCode() {
        return rollbackReasonCode;
    }

    public void setRollbackReasonCode(String rollbackReasonCode) {
        this.rollbackReasonCode = rollbackReasonCode;
    }

    public String getRollbackReasonDetail() {
        return rollbackReasonDetail;
    }

    public void setRollbackReasonDetail(String rollbackReasonDetail) {
        this.rollbackReasonDetail = rollbackReasonDetail;
    }

    public String getRollbackRequestedBy() {
        return rollbackRequestedBy;
    }

    public void setRollbackRequestedBy(String rollbackRequestedBy) {
        this.rollbackRequestedBy = rollbackRequestedBy;
    }

    public Instant getRollbackRequestedAt() {
        return rollbackRequestedAt;
    }

    public void setRollbackRequestedAt(Instant rollbackRequestedAt) {
        this.rollbackRequestedAt = rollbackRequestedAt;
    }

    public String getRollbackSupportingReference() {
        return rollbackSupportingReference;
    }

    public void setRollbackSupportingReference(String rollbackSupportingReference) {
        this.rollbackSupportingReference = rollbackSupportingReference;
    }

    public String getRollbackReviewedBy() {
        return rollbackReviewedBy;
    }

    public void setRollbackReviewedBy(String rollbackReviewedBy) {
        this.rollbackReviewedBy = rollbackReviewedBy;
    }

    public Instant getRollbackReviewedAt() {
        return rollbackReviewedAt;
    }

    public void setRollbackReviewedAt(Instant rollbackReviewedAt) {
        this.rollbackReviewedAt = rollbackReviewedAt;
    }

    public String getRollbackReviewNote() {
        return rollbackReviewNote;
    }

    public void setRollbackReviewNote(String rollbackReviewNote) {
        this.rollbackReviewNote = rollbackReviewNote;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Instant refundedAt) {
        this.refundedAt = refundedAt;
    }

    public UUID getRefundTransactionId() {
        return refundTransactionId;
    }

    public void setRefundTransactionId(UUID refundTransactionId) {
        this.refundTransactionId = refundTransactionId;
    }

    public UUID getRefundedForTransactionId() {
        return refundedForTransactionId;
    }

    public void setRefundedForTransactionId(UUID refundedForTransactionId) {
        this.refundedForTransactionId = refundedForTransactionId;
    }

    public List<AlertReferenceResponse> getTriggeredAlerts() {
        return triggeredAlerts;
    }

    public void setTriggeredAlerts(List<AlertReferenceResponse> triggeredAlerts) {
        this.triggeredAlerts = triggeredAlerts;
    }
}

