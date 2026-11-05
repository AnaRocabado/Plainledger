package com.plainledger.architecture.fixtures.compliant.billing.service;

import com.plainledger.architecture.fixtures.compliant.orders.domain.Order;

/** Uses another feature's domain type: allowed. */
public class BillingService {

    public String bill(Order order) {
        return order.id();
    }
}
