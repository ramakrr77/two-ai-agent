package com.twokey.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A refund created by the Policy Gate, either completed automatically or waiting for a human. */
public record Refund(String id, String orderId, String paymentId, BigDecimal amount,
                     RefundStatus status, String reason, Instant createdAt) {

    public Refund withStatus(RefundStatus newStatus) {
        return new Refund(id, orderId, paymentId, amount, newStatus, reason, createdAt);
    }
}
