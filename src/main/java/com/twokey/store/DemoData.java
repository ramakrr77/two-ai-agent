package com.twokey.store;

import com.twokey.domain.Order;
import com.twokey.domain.Payment;
import com.twokey.domain.PaymentStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sample customers, orders and payments so the demo works out of the box.
 *
 * <pre>
 * C-100 (Alice)  A-1042  headphones     $49.99  charged twice, 2 min apart  -> auto refund
 * C-100 (Alice)  A-2001  coffee grinder $89.00  charged once                -> nothing to refund
 * C-100 (Alice)  A-3003  standing desk  $899.00 charged twice              -> needs a human
 * C-200 (Bob)    B-5005  running shoes  $120.00 charged twice              -> Alice can't touch it
 * </pre>
 */
@Component
public class DemoData {

    public static final String ALICE = "C-100";
    public static final String BOB = "C-200";

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final AuditLog audit;

    public DemoData(OrderRepository orders, PaymentRepository payments, RefundRepository refunds, AuditLog audit) {
        this.orders = orders;
        this.payments = payments;
        this.refunds = refunds;
        this.audit = audit;
    }

    @PostConstruct
    public void load() {
        reset();
    }

    public synchronized void reset() {
        orders.clear();
        payments.clear();
        refunds.clear();
        audit.clear();

        Instant base = Instant.parse("2026-10-01T10:00:00Z");

        order("A-1042", ALICE, "Wireless headphones", "49.99");
        payment("P-1", "A-1042", "49.99", base);
        payment("P-2", "A-1042", "49.99", base.plusSeconds(120));          // duplicate

        order("A-2001", ALICE, "Coffee grinder", "89.00");
        payment("P-3", "A-2001", "89.00", base.plusSeconds(3600));

        order("A-3003", ALICE, "Standing desk", "899.00");
        payment("P-4", "A-3003", "899.00", base.plusSeconds(7200));
        payment("P-5", "A-3003", "899.00", base.plusSeconds(7290));         // duplicate, above limit

        order("B-5005", BOB, "Running shoes", "120.00");
        payment("P-6", "B-5005", "120.00", base.plusSeconds(10800));
        payment("P-7", "B-5005", "120.00", base.plusSeconds(10860));        // duplicate, Bob's order

        audit.record("system", "DEMO_DATA_LOADED", "4 orders and 7 payments loaded");
    }

    private void order(String id, String customerId, String description, String total) {
        orders.save(new Order(id, customerId, description, new BigDecimal(total)));
    }

    private void payment(String id, String orderId, String amount, Instant chargedAt) {
        payments.save(new Payment(id, orderId, new BigDecimal(amount), chargedAt, PaymentStatus.CAPTURED));
    }
}
