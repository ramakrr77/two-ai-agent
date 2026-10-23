package com.twokey.agent;

import com.twokey.policy.Decision;
import com.twokey.policy.PolicyGate;
import com.twokey.store.AuditLog;
import com.twokey.store.OrderRepository;
import com.twokey.store.PaymentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Key 1 without an LLM: a tiny rule-based planner, so the demo runs with no API key.
 *
 * <p>It follows the same steps the LLM is told to follow (find order, list payments, pick the
 * duplicate, request a refund) and uses exactly the same tools, so the Policy Gate still decides.
 * Swap it for {@link LlmRefundAgent} with {@code twokey.agent.mode=llm}.
 */
@Service
@ConditionalOnProperty(name = "twokey.agent.mode", havingValue = "offline", matchIfMissing = true)
public class OfflineRefundAgent implements RefundAgent {

    private static final Pattern ORDER_NUMBER = Pattern.compile("\\b([A-Z]-\\d{4})\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> REFUND_WORDS = List.of("twice", "duplicate", "double", "refund", "charged");

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PolicyGate policyGate;
    private final AuditLog audit;

    public OfflineRefundAgent(OrderRepository orders, PaymentRepository payments, PolicyGate policyGate, AuditLog audit) {
        this.orders = orders;
        this.payments = payments;
        this.policyGate = policyGate;
        this.audit = audit;
    }

    @Override
    public AgentReply handle(String customerId, String message) {
        audit.record("customer:" + customerId, "MESSAGE", message);
        String reply = plan(customerId, message);
        audit.record("agent(for " + customerId + ")", "REPLY", reply);
        return new AgentReply(reply, "offline");
    }

    private String plan(String customerId, String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (REFUND_WORDS.stream().noneMatch(text::contains)) {
            return "I can help if you were charged twice for an order. Tell me the order number, for example A-1042.";
        }

        Matcher matcher = ORDER_NUMBER.matcher(message);
        if (!matcher.find()) {
            return "I can look into that. What is your order number? It looks like A-1042.";
        }
        String orderId = matcher.group(1).toUpperCase(Locale.ROOT);

        RefundTools tools = new RefundTools(customerId, orders, payments, policyGate, audit);

        OrderLookup order = tools.findOrder(orderId);
        if (!order.found()) {
            return "I couldn't find order %s on your account. Please check the order number.".formatted(orderId);
        }

        List<PaymentView> charges = tools.listPayments(orderId);
        Optional<PaymentView> duplicate = laterIdenticalCharge(charges);
        if (duplicate.isEmpty()) {
            return "I checked order %s (%s). I see %d charge(s) and no duplicate, so there is nothing to refund."
                    .formatted(orderId, order.description(), charges.size());
        }

        Decision decision = tools.requestRefund(orderId, duplicate.get().paymentId());
        return switch (decision.outcome()) {
            case APPROVED -> ("Done! I found a duplicate charge of $%s on order %s (%s) and refunded it "
                    + "(refund %s). You'll see it on your card in 3-5 business days.")
                    .formatted(decision.amount(), orderId, order.description(), decision.refundId());
            case NEEDS_HUMAN_APPROVAL -> ("I found a duplicate charge of $%s on order %s. Because of the amount, "
                    + "a specialist needs to approve it (request %s). You'll hear back within one business day.")
                    .formatted(decision.amount(), orderId, decision.refundId());
            case DENIED -> "I couldn't refund this charge: %s.".formatted(decision.reason());
        };
    }

    /** The planner's guess: the latest charge that has an identical earlier charge. The gate still decides. */
    private Optional<PaymentView> laterIdenticalCharge(List<PaymentView> charges) {
        for (int i = charges.size() - 1; i > 0; i--) {
            PaymentView later = charges.get(i);
            for (int j = 0; j < i; j++) {
                if (charges.get(j).amount().compareTo(later.amount()) == 0) {
                    return Optional.of(later);
                }
            }
        }
        return Optional.empty();
    }
}
