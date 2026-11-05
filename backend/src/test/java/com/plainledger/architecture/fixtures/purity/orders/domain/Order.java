package com.plainledger.architecture.fixtures.purity.orders.domain;

import org.springframework.stereotype.Component;

/** Violation: the domain must not depend on Spring. */
@Component
public class Order {}
