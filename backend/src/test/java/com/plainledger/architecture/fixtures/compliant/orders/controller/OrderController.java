package com.plainledger.architecture.fixtures.compliant.orders.controller;

import com.plainledger.architecture.fixtures.compliant.orders.domain.Order;
import com.plainledger.architecture.fixtures.compliant.orders.service.OrderService;

public class OrderController {

    private final OrderService service = new OrderService();

    public Order get(String id) {
        return service.get(id);
    }
}
