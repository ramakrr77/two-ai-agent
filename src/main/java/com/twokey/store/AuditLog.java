package com.twokey.store;

import com.twokey.domain.AuditEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Records every question, plan, check and action, so anyone can answer "why did the AI do that?".
 * In production, write these events to a durable, append-only store.
 */
@Component
public class AuditLog {

    private final List<AuditEvent> events = new ArrayList<>();

    public synchronized void record(String actor, String action, String details) {
        events.add(new AuditEvent(Instant.now(), actor, action, details));
    }

    public synchronized List<AuditEvent> all() {
        return List.copyOf(events);
    }

    public synchronized void clear() {
        events.clear();
    }
}
