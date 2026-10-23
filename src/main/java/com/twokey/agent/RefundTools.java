package com.twokey.agent;

import com.twokey.policy.Decision;
import com.twokey.policy.PolicyGate;
import com.twokey.store.AuditLog;
import com.twokey.store.OrderRepository;
import com.twokey.store.PaymentRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * The ONLY things the AI agent can do. Created per request for one customer, so the AI can never
 * choose whose data it sees.
 *
 * <ul>
 *   <li>{@link #findOrder} and {@link #listPayments} are read-only: look freely, change nothing.</li>
 *   <li>{@link #requestRefund} changes money, so it goes through the {@link PolicyGate} (Key 2).
 *       Note it takes no amount: the amount always comes from the payment system.</li>
 * </ul>
 */
public class RefundTools {

    private final String customerId;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PolicyGate policyGate;
    private final AuditLog audit;

    public RefundTools(String customerId, OrderRepository orders, PaymentRepository payments,
                       PolicyGate policyGate, AuditLog audit) {
        this.customerId = customerId;
        this.orders = orders;
        this.payments = payments;
        this.policyGate = policyGate;
        this.audit = audit;
    }

    @Tool(description = "Find an order by its order number. Only finds orders that belong to the current customer.")
    public OrderLookup findOrder(@ToolParam(description = "Order number, for example A-1042") String orderId) {
        audit.record(actor(), "TOOL_FIND_ORDER", "order=" + orderId);
        return orders.findById(orderId)
                .filter(order -> order.belongsTo(customerId))
                .map(OrderLookup::of)
                .orElseGet(() -> OrderLookup.notFound(orderId));
    }

    @Tool(description = "List all card payments charged for one of the current customer's orders, oldest first.")
    public List<PaymentView> listPayments(@ToolParam(description = "Order number, for example A-1042") String orderId) {
        audit.record(actor(), "TOOL_LIST_PAYMENTS", "order=" + orderId);
        return orders.findById(orderId)
                .filter(order -> order.belongsTo(customerId))
                .map(order -> payments.findByOrderId(order.id()).stream().map(PaymentView::of).toList())
                .orElse(List.of());
    }

    @Tool(description = "Ask to refund ONE duplicate payment. The Policy Gate checks the business rules and decides. "
            + "Report its outcome and reason to the customer exactly; never promise a refund it did not approve.")
    public Decision requestRefund(
            @ToolParam(description = "Order number, for example A-1042") String orderId,
            @ToolParam(description = "Payment ID of the duplicate charge, for example P-2") String paymentId) {
        audit.record(actor(), "TOOL_REQUEST_REFUND", "order=%s payment=%s".formatted(orderId, paymentId));
        return policyGate.tryRefund(customerId, orderId, paymentId);
    }

    private String actor() {
        return "agent(for " + customerId + ")";
    }
}
