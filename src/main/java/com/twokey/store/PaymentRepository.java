package com.twokey.store;

import com.twokey.domain.Payment;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory stand-in for a payment service. Replace with your real payment API. */
@Repository
public class PaymentRepository {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();

    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(id == null ? null : payments.get(id.trim().toUpperCase()));
    }

    public List<Payment> findByOrderId(String orderId) {
        return payments.values().stream()
                .filter(p -> p.orderId().equals(orderId))
                .sorted(Comparator.comparing(Payment::chargedAt))
                .toList();
    }

    public void save(Payment payment) {
        payments.put(payment.id(), payment);
    }

    public void clear() {
        payments.clear();
    }
}
