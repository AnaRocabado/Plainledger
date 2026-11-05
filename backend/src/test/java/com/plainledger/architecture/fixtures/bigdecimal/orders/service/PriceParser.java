package com.plainledger.architecture.fixtures.bigdecimal.orders.service;

import java.math.BigDecimal;

/** Violations: BigDecimal created from a double (outside the domain, too). */
public class PriceParser {

    public BigDecimal fromConstructor(double value) {
        return new BigDecimal(value);
    }

    public BigDecimal fromValueOf(double value) {
        return BigDecimal.valueOf(value);
    }
}
