package com.twokey.agent;

import com.twokey.domain.Order;

import java.math.BigDecimal;

/** Read-only view of an order returned to the AI by the findOrder tool. */
public record OrderLookup(boolean found, String orderId, String description, BigDecimal total, String message) {

    static OrderLookup of(Order order) {
        return new OrderLookup(true, order.id(), order.description(), order.total(), "Order found");
    }

    static OrderLookup notFound(String orderId) {
        return new OrderLookup(false, orderId, null, null, "No order with this number on the customer's account");
    }
}
