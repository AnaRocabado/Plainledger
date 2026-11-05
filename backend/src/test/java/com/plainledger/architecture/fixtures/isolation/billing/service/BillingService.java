package com.plainledger.architecture.fixtures.isolation.billing.service;

import com.plainledger.architecture.fixtures.isolation.orders.controller.OrderController;
import com.plainledger.architecture.fixtures.isolation.orders.repository.OrderRepository;

/** Violation: uses another feature's repository and controller instead of its service. */
public class BillingService {

    private final OrderRepository orders = new OrderRepository();
    private final OrderController controller = new OrderController();

    public String bill(String id) {
        return orders.findById(id) + controller.get(id);
    }
}
