package com.plainledger.architecture.fixtures.bigdecimal.orders.service;

import java.math.BigDecimal;
import java.math.MathContext;

/** Violations: BigDecimal created from a double (outside the domain, too). */
public class PriceParser {

    public BigDecimal fromConstructor(double value) {
        return new BigDecimal(value);
    }

    public BigDecimal fromConstructorWithContext(double value) {
        return new BigDecimal(value, MathContext.DECIMAL64);
    }

    public BigDecimal fromValueOf(double value) {
        return BigDecimal.valueOf(value);
    }
}
