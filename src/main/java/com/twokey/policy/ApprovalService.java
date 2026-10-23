package com.twokey.policy;

import com.twokey.domain.Payment;
import com.twokey.domain.PaymentStatus;
import com.twokey.domain.Refund;
import com.twokey.domain.RefundStatus;
import com.twokey.store.AuditLog;
import com.twokey.store.PaymentRepository;
import com.twokey.store.RefundRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/** The extra human key: a person approves or rejects refunds the Policy Gate sent for review. */
@Service
public class ApprovalService {

    private final RefundRepository refunds;
    private final PaymentRepository payments;
    private final AuditLog audit;

    public ApprovalService(RefundRepository refunds, PaymentRepository payments, AuditLog audit) {
        this.refunds = refunds;
        this.payments = payments;
        this.audit = audit;
    }

    public List<Refund> pending() {
        return refunds.findByStatus(RefundStatus.PENDING_APPROVAL);
    }

    public synchronized Refund approve(String refundId, String approver) {
        Refund refund = pendingRefund(refundId);
        Payment payment = payments.findById(refund.paymentId())
                .orElseThrow(() -> new NoSuchElementException("Payment " + refund.paymentId() + " not found"));

        Refund completed = refund.withStatus(RefundStatus.COMPLETED);
        refunds.save(completed);
        payments.save(payment.withStatus(PaymentStatus.REFUNDED));
        audit.record("human:" + approver, "APPROVED",
                "refund=%s amount=$%s".formatted(refund.id(), refund.amount()));
        return completed;
    }

    public synchronized Refund reject(String refundId, String approver) {
        Refund rejected = pendingRefund(refundId).withStatus(RefundStatus.REJECTED);
        refunds.save(rejected);
        audit.record("human:" + approver, "REJECTED", "refund=" + rejected.id());
        return rejected;
    }

    private Refund pendingRefund(String refundId) {
        Refund refund = refunds.findById(refundId)
                .orElseThrow(() -> new NoSuchElementException("Refund " + refundId + " not found"));
        if (refund.status() != RefundStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Refund " + refundId + " is " + refund.status() + ", not pending approval");
        }
        return refund;
    }
}
