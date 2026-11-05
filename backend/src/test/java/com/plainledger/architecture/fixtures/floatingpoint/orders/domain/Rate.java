package com.plainledger.architecture.fixtures.floatingpoint.orders.domain;

import java.math.BigDecimal;

/** Violations: floating point parameter and return type. */
public class Rate {

    public BigDecimal apply(BigDecimal amount, Double factor) {
        return amount.multiply(new BigDecimal(factor.toString()));
    }

    public float asFloat() {
        return 0;
    }
}
