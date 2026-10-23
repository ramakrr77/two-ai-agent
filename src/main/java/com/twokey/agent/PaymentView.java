package com.twokey.agent;

import com.twokey.domain.Payment;

import java.math.BigDecimal;

/** Read-only view of a payment returned to the AI by the listPayments tool. */
public record PaymentView(String paymentId, BigDecimal amount, String chargedAt, String status) {

    static PaymentView of(Payment payment) {
        return new PaymentView(payment.id(), payment.amount(), payment.chargedAt().toString(), payment.status().name());
    }
}
