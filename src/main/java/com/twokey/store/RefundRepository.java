package com.twokey.store;

import com.twokey.domain.Refund;
import com.twokey.domain.RefundStatus;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory stand-in for a refund service. */
@Repository
public class RefundRepository {

    private final Map<String, Refund> refunds = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public String nextId() {
        return "R-" + (1000 + sequence.incrementAndGet());
    }

    public Optional<Refund> findById(String id) {
        return Optional.ofNullable(id == null ? null : refunds.get(id.trim().toUpperCase()));
    }

    /** True if this payment already has a completed refund or one waiting for approval. */
    public boolean hasActiveRefund(String paymentId) {
        return refunds.values().stream()
                .anyMatch(r -> r.paymentId().equals(paymentId) && r.status() != RefundStatus.REJECTED);
    }

    public List<Refund> findByStatus(RefundStatus status) {
        return refunds.values().stream()
                .filter(r -> r.status() == status)
                .sorted(Comparator.comparing(Refund::createdAt))
                .toList();
    }

    public List<Refund> findByOrderId(String orderId) {
        return refunds.values().stream()
                .filter(r -> r.orderId().equals(orderId))
                .sorted(Comparator.comparing(Refund::createdAt))
                .toList();
    }

    public void save(Refund refund) {
        refunds.put(refund.id(), refund);
    }

    public void clear() {
        refunds.clear();
        sequence.set(0);
    }
}
