package com.hsbc.tms.transaction.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages SSE (Server-Sent Events) subscriptions for real-time transaction status sync.
 *
 * <p>Frontend pages call GET /api/v1/transactions/events to obtain a long-lived SSE stream.
 * Whenever a transaction is created or its status changes, the service broadcasts a
 * "transaction-updated" SSE event to all connected clients AFTER the DB transaction commits
 * (via {@link TransactionalEventListener}).  Each page then re-fetches the affected data so
 * every view stays consistent with the authoritative database state.</p>
 */
@Service
public class TransactionEventService {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventService.class);

    /**
     * 0L means "no server-side timeout" — the browser keeps the connection alive
     * and reconnects automatically when it drops.
     */
    private static final long SSE_TIMEOUT_MS = 0L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // ----------------------------------------------------------------
    // Subscription management
    // ----------------------------------------------------------------

    /**
     * Called by the SSE controller endpoint.  Creates an emitter, registers cleanup
     * callbacks, sends a "connected" heartbeat, and returns it to Spring MVC so it can
     * be streamed to the client.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitters.add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed, removing from registry");
            emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out, removing from registry");
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(ex -> {
            log.debug("SSE emitter error ({}), removing from registry", ex.getMessage());
            emitters.remove(emitter);
        });

        // Send an initial "connected" ping so the client knows the stream is live.
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        log.debug("New SSE subscriber added; total subscribers: {}", emitters.size());
        return emitter;
    }

    // ----------------------------------------------------------------
    // Event broadcasting (fires AFTER the DB transaction commits)
    // ----------------------------------------------------------------

    /**
     * Listens for {@link TransactionUpdatedEvent} published by {@link TransactionServiceImpl}
     * and broadcasts a "transaction-updated" SSE message to every connected client.
     *
     * <p>The {@code AFTER_COMMIT} phase guarantee means clients always receive the event
     * after the row is durably written, so they will see the new status when they re-fetch.</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionUpdated(TransactionUpdatedEvent event) {
        String payload = "{\"id\":\"" + event.getTransactionId()
                + "\",\"status\":\"" + event.getStatus() + "\"}";

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("transaction-updated")
                        .data(payload));
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        }
        if (!dead.isEmpty()) {
            emitters.removeAll(dead);
            log.debug("Removed {} stale SSE emitter(s) after broadcast", dead.size());
        }
        log.debug("Broadcast transaction-updated event for id={} status={}", event.getTransactionId(), event.getStatus());
    }
}

