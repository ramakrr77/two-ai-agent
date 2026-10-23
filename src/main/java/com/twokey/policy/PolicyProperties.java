package com.twokey.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/** Business limits for the Policy Gate, set in application.yml under {@code twokey.policy}. */
@ConfigurationProperties(prefix = "twokey.policy")
public record PolicyProperties(BigDecimal autoRefundLimit, Duration duplicateWindow) {

    public PolicyProperties {
        if (autoRefundLimit == null) {
            autoRefundLimit = new BigDecimal("200.00");
        }
        if (duplicateWindow == null) {
            duplicateWindow = Duration.ofMinutes(10);
        }
    }
}
