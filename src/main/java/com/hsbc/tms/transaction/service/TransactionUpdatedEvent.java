package com.hsbc.tms.transaction.service;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent published after a transaction is created or its status changes.
 * Listeners annotated with @TransactionalEventListener(phase = AFTER_COMMIT) will receive
 * this event only once the database transaction has successfully committed, so SSE subscribers
 * always see committed data when they re-fetch.
 */
public class TransactionUpdatedEvent extends ApplicationEvent {

    private final UUID transactionId;
    private final String status;

    public TransactionUpdatedEvent(Object source, UUID transactionId, String status) {
        super(source);
        this.transactionId = transactionId;
        this.status = status;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }
}

