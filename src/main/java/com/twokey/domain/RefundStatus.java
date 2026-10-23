package com.twokey.domain;

public enum RefundStatus {
    /** Approved by the Policy Gate (or a human) and sent back to the card. */
    COMPLETED,
    /** Above the automatic limit; waiting for the human key. */
    PENDING_APPROVAL,
    /** A human reviewed it and said no. */
    REJECTED
}
