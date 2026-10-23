package com.twokey.domain;

import java.math.BigDecimal;

/** A customer order, as stored in the order system. */
public record Order(String id, String customerId, String description, BigDecimal total) {

    public boolean belongsTo(String customerId) {
        return this.customerId.equals(customerId);
    }
}
