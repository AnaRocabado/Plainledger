package com.plainledger.architecture.fixtures.floatingpoint.orders.domain;

/** Violation: binary floating point field. */
public class Price {

    private final double amount;

    public Price(String amount) {
        this.amount = Double.parseDouble(amount);
    }

    public String format() {
        return String.valueOf(amount);
    }
}
