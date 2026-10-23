package com.twokey.policy;

import com.twokey.domain.PaymentStatus;
import com.twokey.domain.RefundStatus;
import com.twokey.store.AuditLog;
import com.twokey.store.DemoData;
import com.twokey.store.OrderRepository;
import com.twokey.store.PaymentRepository;
import com.twokey.store.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static com.twokey.policy.Decision.Outcome.APPROVED;
import static com.twokey.policy.Decision.Outcome.DENIED;
import static com.twokey.policy.Decision.Outcome.NEEDS_HUMAN_APPROVAL;
import static com.twokey.store.DemoData.ALICE;
import static org.assertj.core.api.Assertions.assertThat;

/** Key 2 on its own: plain Java rules, no AI, no Spring context. */
class PolicyGateTest {

    private PaymentRepository payments;
    private RefundRepository refunds;
    private AuditLog audit;
    private PolicyGate gate;

    @BeforeEach
    void setUp() {
        OrderRepository orders = new OrderRepository();
        payments = new PaymentRepository();
        refunds = new RefundRepository();
        audit = new AuditLog();
        new DemoData(orders, payments, refunds, audit).reset();
        gate = new PolicyGate(orders, payments, refunds, audit,
                new PolicyProperties(new BigDecimal("200.00"), Duration.ofMinutes(10)));
    }

    @Test
    void refundsADuplicateChargeUnderTheLimit() {
        Decision decision = gate.tryRefund(ALICE, "A-1042", "P-2");

        assertThat(decision.outcome()).isEqualTo(APPROVED);
        assertThat(decision.amount()).isEqualByComparingTo("49.99");
        assertThat(payments.findById("P-2").orElseThrow().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(audit.all()).anyMatch(e -> e.action().equals("AUTO_REFUND"));
    }

    @Test
    void refusesTheOriginalPaymentBecauseItIsNotTheDuplicate() {
        Decision decision = gate.tryRefund(ALICE, "A-1042", "P-1");

        assertThat(decision.outcome()).isEqualTo(DENIED);
        assertThat(decision.reason()).contains("not a duplicate");
        assertThat(payments.findById("P-1").orElseThrow().status()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void refusesASinglePayment() {
        assertThat(gate.tryRefund(ALICE, "A-2001", "P-3").outcome()).isEqualTo(DENIED);
    }

    @Test
    void refusesSomeoneElsesOrder() {
        Decision decision = gate.tryRefund(ALICE, "B-5005", "P-7");

        assertThat(decision.outcome()).isEqualTo(DENIED);
        assertThat(decision.reason()).contains("not found on this customer's account");
        assertThat(payments.findById("P-7").orElseThrow().status()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void refusesAPaymentFromADifferentOrder() {
        Decision decision = gate.tryRefund(ALICE, "A-1042", "P-5");

        assertThat(decision.outcome()).isEqualTo(DENIED);
        assertThat(decision.reason()).contains("Payment not found on this order");
    }

    @Test
    void neverRefundsTheSamePaymentTwice() {
        assertThat(gate.tryRefund(ALICE, "A-1042", "P-2").outcome()).isEqualTo(APPROVED);

        Decision second = gate.tryRefund(ALICE, "A-1042", "P-2");

        assertThat(second.outcome()).isEqualTo(DENIED);
        assertThat(second.reason()).contains("already refunded");
        assertThat(refunds.findByOrderId("A-1042")).hasSize(1);
    }

    @Test
    void sendsBigRefundsToAHuman() {
        Decision decision = gate.tryRefund(ALICE, "A-3003", "P-5");

        assertThat(decision.outcome()).isEqualTo(NEEDS_HUMAN_APPROVAL);
        assertThat(decision.amount()).isEqualByComparingTo("899.00");
        assertThat(refunds.findByStatus(RefundStatus.PENDING_APPROVAL)).hasSize(1);
        // Money has not moved yet.
        assertThat(payments.findById("P-5").orElseThrow().status()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void chargesOutsideTheWindowAreNotDuplicates() {
        PolicyGate strictGate = new PolicyGate(new OrderRepository(), payments, refunds, audit,
                new PolicyProperties(new BigDecimal("200.00"), Duration.ofSeconds(30)));
        // P-2 came 120 seconds after P-1, outside a 30-second window.
        assertThat(strictGate.isDuplicate(payments.findById("P-2").orElseThrow())).isFalse();
    }
}
