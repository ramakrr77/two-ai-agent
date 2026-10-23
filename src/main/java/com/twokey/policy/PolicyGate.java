package com.twokey.policy;

import com.twokey.domain.Order;
import com.twokey.domain.Payment;
import com.twokey.domain.PaymentStatus;
import com.twokey.domain.Refund;
import com.twokey.domain.RefundStatus;
import com.twokey.store.AuditLog;
import com.twokey.store.OrderRepository;
import com.twokey.store.PaymentRepository;
import com.twokey.store.RefundRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Key 2: plain Java rules that decide IF a refund is allowed.
 *
 * <p>No AI runs in here. The AI can only ask; this class decides. Every rule is readable,
 * unit-tested and audited, and the refund amount always comes from the payment system.
 */
@Service
public class PolicyGate {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final AuditLog audit;
    private final PolicyProperties policy;

    public PolicyGate(OrderRepository orders, PaymentRepository payments, RefundRepository refunds,
                      AuditLog audit, PolicyProperties policy) {
        this.orders = orders;
        this.payments = payments;
        this.refunds = refunds;
        this.audit = audit;
        this.policy = policy;
    }

    /**
     * Tries to refund one payment for the given customer. Synchronized so two requests can never
     * refund the same payment twice.
     */
    public synchronized Decision tryRefund(String customerId, String orderId, String paymentId) {
        String request = "customer=%s order=%s payment=%s".formatted(customerId, orderId, paymentId);
        audit.record("policy-gate", "CHECK_STARTED", request);

        // Rule 1: the order must exist and belong to this customer.
        Optional<Order> order = orders.findById(orderId);
        if (order.isEmpty() || !order.get().belongsTo(customerId)) {
            return deny(request, "Order not found on this customer's account");
        }

        // Rule 2: the payment must belong to that order.
        Optional<Payment> found = payments.findById(paymentId);
        if (found.isEmpty() || !found.get().orderId().equals(order.get().id())) {
            return deny(request, "Payment not found on this order");
        }
        Payment payment = found.get();

        // Rule 3: never refund the same payment twice.
        if (payment.status() == PaymentStatus.REFUNDED || refunds.hasActiveRefund(payment.id())) {
            return deny(request, "This payment was already refunded or is waiting for approval");
        }

        // Rule 4: it must really be a duplicate (an identical earlier charge within the window).
        if (!isDuplicate(payment)) {
            return deny(request, "This payment is not a duplicate charge");
        }

        // The amount comes from the payment system, never from the AI.
        BigDecimal amount = payment.amount();

        // Rule 5: big refunds need the human key.
        if (amount.compareTo(policy.autoRefundLimit()) > 0) {
            Refund pending = createRefund(order.get(), payment, RefundStatus.PENDING_APPROVAL,
                    "Duplicate charge above automatic limit of $" + policy.autoRefundLimit());
            audit.record("policy-gate", "SENT_TO_HUMAN",
                    "%s amount=$%s refund=%s".formatted(request, amount, pending.id()));
            return Decision.needsHuman(pending.id(), amount,
                    "Amount is above the automatic refund limit; a specialist will review it");
        }

        // Both keys turned: issue the refund.
        Refund refund = createRefund(order.get(), payment, RefundStatus.COMPLETED, "Duplicate charge");
        payments.save(payment.withStatus(PaymentStatus.REFUNDED));
        audit.record("policy-gate", "AUTO_REFUND",
                "%s amount=$%s refund=%s".formatted(request, amount, refund.id()));
        return Decision.approved(refund.id(), amount);
    }

    /** A payment is a duplicate if the same order has an identical, earlier charge inside the window. */
    boolean isDuplicate(Payment payment) {
        Duration window = policy.duplicateWindow();
        return payments.findByOrderId(payment.orderId()).stream()
                .filter(other -> !other.id().equals(payment.id()))
                .filter(other -> other.amount().compareTo(payment.amount()) == 0)
                .anyMatch(other -> other.chargedAt().isBefore(payment.chargedAt())
                        && Duration.between(other.chargedAt(), payment.chargedAt()).compareTo(window) <= 0);
    }

    private Refund createRefund(Order order, Payment payment, RefundStatus status, String reason) {
        Refund refund = new Refund(refunds.nextId(), order.id(), payment.id(), payment.amount(),
                status, reason, Instant.now());
        refunds.save(refund);
        return refund;
    }

    private Decision deny(String request, String reason) {
        audit.record("policy-gate", "DENIED", request + " reason=" + reason);
        return Decision.denied(reason);
    }
}
