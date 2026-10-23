package com.twokey.web;

import com.twokey.domain.AuditEvent;
import com.twokey.domain.Order;
import com.twokey.domain.Payment;
import com.twokey.domain.Refund;
import com.twokey.store.AuditLog;
import com.twokey.store.DemoData;
import com.twokey.store.OrderRepository;
import com.twokey.store.PaymentRepository;
import com.twokey.store.RefundRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only views for the demo page, plus a reset button. Not something you would expose in production. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    public record OrderDetails(Order order, List<Payment> payments, List<Refund> refunds) {
    }

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final AuditLog audit;
    private final DemoData demoData;

    public DemoController(OrderRepository orders, PaymentRepository payments, RefundRepository refunds,
                          AuditLog audit, DemoData demoData) {
        this.orders = orders;
        this.payments = payments;
        this.refunds = refunds;
        this.audit = audit;
        this.demoData = demoData;
    }

    @GetMapping("/customers/{customerId}/orders")
    public List<OrderDetails> orders(@PathVariable String customerId) {
        return orders.findByCustomerId(customerId.trim().toUpperCase()).stream()
                .map(o -> new OrderDetails(o, payments.findByOrderId(o.id()), refunds.findByOrderId(o.id())))
                .toList();
    }

    @GetMapping("/audit")
    public List<AuditEvent> audit() {
        return audit.all();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        demoData.reset();
        return ResponseEntity.noContent().build();
    }
}
