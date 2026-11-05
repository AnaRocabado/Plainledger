package com.plainledger.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plainledger.testsupport.Verifies;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Applies {@link ArchitectureRules} to the production code, and proves with fixtures under
 * {@code architecture.fixtures} that each rule detects the violation it exists for.
 */
class ArchitectureTest {

    private static final String FIXTURES = "com.plainledger.architecture.fixtures.";

    @Test
    @Verifies({"FOUND-AC-03", "FOUND-AC-04"})
    void productionCodeFollowsAllRules() {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.plainledger");
        ArchitectureRules.ALL.forEach(rule -> rule.check(production));
    }

    @Test
    @Verifies({"FOUND-AC-03", "FOUND-AC-04"})
    void compliantCodePassesAllRules() {
        JavaClasses compliant = fixture("compliant");
        ArchitectureRules.ALL.forEach(
                rule -> assertThatCode(() -> rule.check(compliant)).doesNotThrowAnyException());
    }

    @Nested
    @Verifies("FOUND-AC-03")
    class Layering {

        @Test
        void controllerUsingARepositoryIsAViolation() {
            assertViolation(ArchitectureRules.LAYER_DIRECTION, "layering", "OrderController", "OrderRepository");
        }

        @Test
        void featureUsingAnotherFeaturesRepositoryIsAViolation() {
            assertViolation(
                    ArchitectureRules.FEATURES_USE_EACH_OTHER_THROUGH_SERVICES,
                    "isolation",
                    "BillingService",
                    "OrderRepository");
        }

        @Test
        void featureUsingAnotherFeaturesControllerIsAViolation() {
            assertViolation(
                    ArchitectureRules.FEATURES_USE_EACH_OTHER_THROUGH_SERVICES,
                    "isolation",
                    "BillingService",
                    "OrderController");
        }
    }

    @Nested
    @Verifies("FOUND-AC-04")
    class DomainAndArithmetic {

        @Test
        void domainDependingOnSpringIsAViolation() {
            assertViolation(ArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE, "purity", "Order", "springframework");
        }

        @Test
        void floatingPointFieldInDomainIsAViolation() {
            assertViolation(ArchitectureRules.DOMAIN_HAS_NO_FLOATING_POINT, "floatingpoint", "Price.amount");
        }

        @Test
        void floatingPointParameterOrReturnTypeInDomainIsAViolation() {
            assertViolation(
                    ArchitectureRules.DOMAIN_HAS_NO_FLOATING_POINT, "floatingpoint", "Rate.apply", "Rate.asFloat");
        }

        @Test
        void bigDecimalFromDoubleIsAViolationAnywhere() {
            assertViolation(
                    ArchitectureRules.NO_BIG_DECIMAL_FROM_DOUBLE,
                    "bigdecimal",
                    "PriceParser.fromConstructor",
                    "PriceParser.fromValueOf");
        }
    }

    private static JavaClasses fixture(String name) {
        return new ClassFileImporter().importPackages(FIXTURES + name);
    }

    private static void assertViolation(ArchRule rule, String fixture, String... expectedInMessage) {
        assertThatThrownBy(() -> rule.check(fixture(fixture)))
                .isInstanceOf(AssertionError.class)
                .message()
                .contains(expectedInMessage);
    }
}
