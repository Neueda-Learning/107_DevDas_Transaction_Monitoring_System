package com.hsbc.tms.transaction.model;

public enum TransactionStatus {
    COMPLETED,
    PENDING,
    FAILED,
    PENDING_APPROVAL,
    REJECTED,
    ROLLBACK_REQUESTED,
    ROLLBACK_REJECTED,
    REFUNDED
}

