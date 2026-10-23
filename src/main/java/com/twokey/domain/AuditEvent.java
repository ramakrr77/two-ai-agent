package com.twokey.domain;

import java.time.Instant;

/** One line in the audit log: who did what, and why. */
public record AuditEvent(Instant at, String actor, String action, String details) {
}
