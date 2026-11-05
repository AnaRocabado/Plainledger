package com.plainledger.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Records the outcome of every test annotated with {@link Verifies} and writes it as traceability
 * evidence ({@code plainledger-evidence/v1} JSON) when the test run finishes.
 *
 * <p>Registered through {@code META-INF/services}, so it observes every JUnit Platform engine
 * (Jupiter, jqwik, ...). It writes only when the {@value #SUITE_PROPERTY} system property is set,
 * which Surefire and Failsafe do; IDE runs therefore leave no partial evidence behind.
 */
public final class TraceabilityListener implements TestExecutionListener {

    static final String FORMAT = "plainledger-evidence/v1";
    static final String SUITE_PROPERTY = "plainledger.traceability.suite";
    static final String DIR_PROPERTY = "plainledger.traceability.dir";

    private final Optional<String> suite;
    private final Path outputDir;
    private final List<Result> results = Collections.synchronizedList(new ArrayList<>());

    /** Used by the JUnit Platform: configured through system properties. */
    public TraceabilityListener() {
        this(
                Optional.ofNullable(System.getProperty(SUITE_PROPERTY)),
                Path.of(System.getProperty(DIR_PROPERTY, "target/traceability")));
    }

    TraceabilityListener(Optional<String> suite, Path outputDir) {
        this.suite = suite;
        this.outputDir = outputDir;
    }

    @Override
    public void executionSkipped(TestIdentifier test, String reason) {
        record(test, "skipped");
    }

    @Override
    public void executionFinished(TestIdentifier test, TestExecutionResult result) {
        record(
                test,
                switch (result.getStatus()) {
                    case SUCCESSFUL -> "passed";
                    case ABORTED -> "skipped"; // a failed assumption is not evidence
                    case FAILED -> "failed";
                });
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        suite.ifPresent(name -> write(name, List.copyOf(results)));
    }

    private void record(TestIdentifier test, String status) {
        if (!test.isTest()) {
            return;
        }
        Set<String> criteria =
                test.getSource().map(TraceabilityListener::criteriaOf).orElse(Set.of());
        if (!criteria.isEmpty()) {
            results.add(new Result(testName(test), test.getDisplayName(), status, List.copyOf(criteria)));
        }
    }

    private static Set<String> criteriaOf(TestSource source) {
        Set<String> criteria = new LinkedHashSet<>();
        try {
            if (source instanceof MethodSource method) {
                collect(method.getJavaMethod(), criteria);
                collectFromClassHierarchy(method.getJavaClass(), criteria);
            } else if (source instanceof ClassSource type) {
                collectFromClassHierarchy(type.getJavaClass(), criteria);
            }
        } catch (RuntimeException notLoadable) {
            // A source that cannot be resolved to a Java element carries no @Verifies.
        }
        return criteria;
    }

    private static void collectFromClassHierarchy(Class<?> type, Set<String> criteria) {
        // @Nested test classes are inner classes and inherit the criteria of their enclosing class.
        for (Class<?> current = type;
                current != null;
                current = isInner(current) ? current.getEnclosingClass() : null) {
            collect(current, criteria);
        }
    }

    private static boolean isInner(Class<?> type) {
        return type.isMemberClass() && !Modifier.isStatic(type.getModifiers());
    }

    private static void collect(AnnotatedElement element, Set<String> criteria) {
        AnnotationSupport.findAnnotation(element, Verifies.class)
                .ifPresent(verifies -> criteria.addAll(List.of(verifies.value())));
    }

    private static String testName(TestIdentifier test) {
        return test.getSource()
                .filter(MethodSource.class::isInstance)
                .map(MethodSource.class::cast)
                .map(method -> method.getClassName() + "#" + method.getMethodName())
                .orElse(test.getUniqueId());
    }

    private void write(String suiteName, List<Result> snapshot) {
        StringBuilder json = new StringBuilder()
                .append("{\n  \"format\": ")
                .append(quote(FORMAT))
                .append(",\n  \"suite\": ")
                .append(quote(suiteName))
                .append(",\n  \"results\": [");
        for (int i = 0; i < snapshot.size(); i++) {
            Result r = snapshot.get(i);
            json.append(i == 0 ? "\n" : ",\n")
                    .append("    {\"test\": ")
                    .append(quote(r.test()))
                    .append(", \"name\": ")
                    .append(quote(r.name()))
                    .append(", \"status\": ")
                    .append(quote(r.status()))
                    .append(", \"criteria\": [");
            for (int j = 0; j < r.criteria().size(); j++) {
                json.append(j == 0 ? "" : ", ").append(quote(r.criteria().get(j)));
            }
            json.append("]}");
        }
        json.append(snapshot.isEmpty() ? "]\n}\n" : "\n  ]\n}\n");
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve(suiteName + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write traceability evidence to " + outputDir, e);
        }
    }

    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private record Result(String test, String name, String status, List<String> criteria) {}
}
