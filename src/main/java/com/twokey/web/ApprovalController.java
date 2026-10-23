package com.twokey.web;

import com.twokey.domain.Refund;
import com.twokey.policy.ApprovalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The human key: support leads review refunds the Policy Gate sent for approval. */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvals;

    public ApprovalController(ApprovalService approvals) {
        this.approvals = approvals;
    }

    @GetMapping
    public List<Refund> pending() {
        return approvals.pending();
    }

    @PostMapping("/{refundId}/approve")
    public Refund approve(@PathVariable String refundId,
                          @RequestHeader(value = "X-Approver", defaultValue = "support-lead") String approver) {
        return approvals.approve(refundId, approver);
    }

    @PostMapping("/{refundId}/reject")
    public Refund reject(@PathVariable String refundId,
                         @RequestHeader(value = "X-Approver", defaultValue = "support-lead") String approver) {
        return approvals.reject(refundId, approver);
    }
}
