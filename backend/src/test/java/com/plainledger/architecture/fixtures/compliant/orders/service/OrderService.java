package com.plainledger.architecture.fixtures.compliant.orders.service;

import com.plainledger.architecture.fixtures.compliant.billing.service.BillingService;
import com.plainledger.architecture.fixtures.compliant.orders.domain.Order;
import com.plainledger.architecture.fixtures.compliant.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orders = new OrderRepository();
    private final BillingService billing = new BillingService();

    public Order get(String id) {
        Order order = orders.findById(id).orElseThrow();
        billing.bill(order);
        return order;
    }
}
