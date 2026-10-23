package com.twokey.store;

import com.twokey.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory stand-in for an order service. Replace with your real order API or database. */
@Repository
public class OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(id == null ? null : orders.get(id.trim().toUpperCase()));
    }

    public List<Order> findByCustomerId(String customerId) {
        return orders.values().stream()
                .filter(o -> o.belongsTo(customerId))
                .sorted(Comparator.comparing(Order::id))
                .toList();
    }

    public void save(Order order) {
        orders.put(order.id(), order);
    }

    public void clear() {
        orders.clear();
    }
}
