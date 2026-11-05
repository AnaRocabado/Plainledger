package com.plainledger.architecture.fixtures.layering.orders.controller;

import com.plainledger.architecture.fixtures.layering.orders.repository.OrderRepository;

/** Violation: skips the service layer. */
public class OrderController {

    private final OrderRepository repository = new OrderRepository();

    public String get(String id) {
        return repository.findById(id);
    }
}
