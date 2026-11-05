package com.plainledger.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

@Verifies("FOUND-AC-09")
class TraceabilityListenerTest {

    @TempDir
    Path outputDir;

    @Test
    void recordsAnnotatedTestsWithTheirStatusAndCriteria() throws IOException {
        run(new TraceabilityListener(Optional.of("sample"), outputDir));

        String evidence = Files.readString(outputDir.resolve("sample.json"));
        assertThat(evidence)
                .contains("\"format\": \"plainledger-evidence/v1\"")
                .contains("\"suite\": \"sample\"")
                .contains("SampleTests#passes\", \"name\": \"passes()\", \"status\": \"passed\", "
                        + "\"criteria\": [\"SAMPLE-AC-01\", \"SAMPLE-AC-09\"]")
                .contains("SampleTests#fails\", \"name\": \"fails()\", \"status\": \"failed\", "
                        + "\"criteria\": [\"SAMPLE-AC-02\", \"SAMPLE-AC-09\"]")
                .contains("SampleTests#disabled\", \"name\": \"disabled()\", \"status\": \"skipped\"")
                .contains("SampleTests#inheritsClassCriteria\", \"name\": \"inheritsClassCriteria()\", "
                        + "\"status\": \"passed\", \"criteria\": [\"SAMPLE-AC-09\"]")
                .contains("SampleTests$Inner#inheritsFromEnclosingClass\", \"name\": \"inheritsFromEnclosingClass()\", "
                        + "\"status\": \"passed\", \"criteria\": [\"SAMPLE-AC-09\"]")
                // SampleTests is a static nested class, not @Nested: it must not inherit this class's criteria.
                .doesNotContain("FOUND-AC-09");
    }

    @Test
    void writesNothingWithoutASuiteName() throws IOException {
        run(new TraceabilityListener(Optional.empty(), outputDir));

        try (var files = Files.list(outputDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void escapesJsonStrings() {
        assertThat(TraceabilityListener.quote("a \"b\" \\ c\n")).isEqualTo("\"a \\\"b\\\" \\\\ c\\n\"");
    }

    private static void run(TraceabilityListener listener) {
        // Auto-registration is disabled so the real (Surefire-configured) listener does not observe
        // this nested run.
        Launcher launcher = LauncherFactory.create(LauncherConfig.builder()
                .enableTestExecutionListenerAutoRegistration(false)
                .build());
        launcher.execute(
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(selectClass(SampleTests.class))
                        .build(),
                listener);
    }

    /** Executed only by the nested launcher above; Surefire skips nested classes. */
    @Verifies("SAMPLE-AC-09")
    static class SampleTests {

        @Test
        @Verifies("SAMPLE-AC-01")
        void passes() {}

        @Test
        @Verifies("SAMPLE-AC-02")
        void fails() {
            throw new AssertionError("expected failure");
        }

        @Test
        @Disabled("expected skip")
        @Verifies("SAMPLE-AC-03")
        void disabled() {}

        @Test
        void inheritsClassCriteria() {}

        @Nested
        class Inner {
            @Test
            void inheritsFromEnclosingClass() {}
        }
    }
}
