package com.twokey.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A card charge, as stored in the payment system. The amount here is the only amount the system trusts. */
public record Payment(String id, String orderId, BigDecimal amount, Instant chargedAt, PaymentStatus status) {

    public Payment withStatus(PaymentStatus newStatus) {
        return new Payment(id, orderId, amount, chargedAt, newStatus);
    }
}
