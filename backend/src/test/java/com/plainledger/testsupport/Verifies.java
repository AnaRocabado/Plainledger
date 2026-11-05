package com.plainledger.testsupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which acceptance criteria a test verifies, for example {@code @Verifies("LEDGER-AC-03")}.
 *
 * <p>On a class, it applies to every test in the class (including {@code @Nested} classes).
 * {@link TraceabilityListener} records the result of every annotated test, and
 * {@code tools/traceability} checks that each acceptance criterion has a passing test (ADR-0009).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Verifies {

    /** Acceptance criterion IDs, such as {@code "FOUND-AC-01"}. */
    String[] value();
}
