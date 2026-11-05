package com.plainledger.architecture.fixtures.compliant.orders.domain;

import java.math.BigDecimal;

public record Order(String id, BigDecimal amount) {

    public static Order of(String id, String amount) {
        return new Order(id, new BigDecimal(amount));
    }
}
