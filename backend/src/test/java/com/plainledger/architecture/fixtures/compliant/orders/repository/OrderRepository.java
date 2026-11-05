package com.plainledger.architecture.fixtures.compliant.orders.repository;

import com.plainledger.architecture.fixtures.compliant.orders.domain.Order;
import java.util.Optional;

public class OrderRepository {

    public Optional<Order> findById(String id) {
        return Optional.of(Order.of(id, "10.50"));
    }
}
