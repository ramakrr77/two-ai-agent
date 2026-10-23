package com.twokey.policy;

import java.math.BigDecimal;

/**
 * The Policy Gate's answer. The agent must report this back to the customer as-is.
 *
 * @param refundId set when a refund was created (completed or pending approval)
 * @param amount   always taken from the payment system, never from the AI
 */
public record Decision(Outcome outcome, String reason, String refundId, BigDecimal amount) {

    public enum Outcome {
        /** Both keys turned: the refund was issued. */
        APPROVED,
        /** Rules passed, but the amount needs the human key. */
        NEEDS_HUMAN_APPROVAL,
        /** A rule failed: nothing changed. */
        DENIED
    }

    public static Decision approved(String refundId, BigDecimal amount) {
        return new Decision(Outcome.APPROVED, "Duplicate charge refunded automatically", refundId, amount);
    }

    public static Decision needsHuman(String refundId, BigDecimal amount, String reason) {
        return new Decision(Outcome.NEEDS_HUMAN_APPROVAL, reason, refundId, amount);
    }

    public static Decision denied(String reason) {
        return new Decision(Outcome.DENIED, reason, null, null);
    }
}
