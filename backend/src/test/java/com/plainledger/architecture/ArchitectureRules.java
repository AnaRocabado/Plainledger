package com.plainledger.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Package and arithmetic rules from ADR-0003 and ADR-0006.
 *
 * <p>Features are packages that contain the layer packages {@code controller}, {@code service},
 * {@code repository} and {@code domain}: in {@code com.plainledger.ledger.service}, the feature is
 * {@code com.plainledger.ledger}. Rules use relative package patterns so they apply equally to
 * production code and to the fixtures that prove each rule catches its violation.
 */
final class ArchitectureRules {

    private static final List<String> LAYERS = List.of("controller", "service", "repository", "domain");
    private static final Set<String> FLOATING_POINT_TYPES =
            Set.of("double", "float", Double.class.getName(), Float.class.getName());

    /** ADR-0003 rule 1: controller → service → repository, never upwards or skipping the service. */
    static final ArchRule LAYER_DIRECTION = CompositeArchRule.of(noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .as("controllers do not use repositories"))
            .and(noClasses()
                    .that()
                    .resideInAPackage("..service..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..controller..")
                    .as("services do not use controllers"))
            .and(noClasses()
                    .that()
                    .resideInAPackage("..repository..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..service..", "..controller..")
                    .as("repositories do not use services or controllers"))
            .allowEmptyShould(true);

    /** ADR-0003 rule 2: the domain is plain Java. */
    static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "..controller..", "..service..", "..repository..")
            .as("domain classes do not depend on frameworks or on other layers")
            .allowEmptyShould(true);

    /** ADR-0003 rule 3: other features are used through their service or domain only. */
    static final ArchRule FEATURES_USE_EACH_OTHER_THROUGH_SERVICES = classes()
            .that(resideInALayer())
            .should(onlyUseOtherFeaturesThroughServiceOrDomain())
            .allowEmptyShould(true);

    /** ADR-0006: no binary floating point in the domain. */
    static final ArchRule DOMAIN_HAS_NO_FLOATING_POINT = classes()
            .that()
            .resideInAPackage("..domain..")
            .should(notUseFloatingPointTypes())
            .allowEmptyShould(true);

    /** ADR-0006: a BigDecimal built from a double already carries the binary rounding error. */
    static final ArchRule NO_BIG_DECIMAL_FROM_DOUBLE = noClasses()
            .should()
            .callConstructor(BigDecimal.class, double.class)
            .orShould()
            .callConstructor(BigDecimal.class, double.class, MathContext.class)
            .orShould()
            .callMethod(BigDecimal.class, "valueOf", double.class)
            .as("no BigDecimal is created from a double")
            .allowEmptyShould(true);

    static final List<ArchRule> ALL = List.of(
            LAYER_DIRECTION,
            DOMAIN_IS_FRAMEWORK_FREE,
            FEATURES_USE_EACH_OTHER_THROUGH_SERVICES,
            DOMAIN_HAS_NO_FLOATING_POINT,
            NO_BIG_DECIMAL_FROM_DOUBLE);

    private ArchitectureRules() {}

    /** Location of a class in the feature/layer structure, if it is inside a layer package. */
    record Location(String feature, String layer) {

        static Optional<Location> of(JavaClass type) {
            List<String> segments = Arrays.asList(type.getPackageName().split("\\."));
            for (int i = 1; i < segments.size(); i++) {
                if (LAYERS.contains(segments.get(i))) {
                    return Optional.of(new Location(String.join(".", segments.subList(0, i)), segments.get(i)));
                }
            }
            return Optional.empty();
        }
    }

    private static DescribedPredicate<JavaClass> resideInALayer() {
        return DescribedPredicate.describe(
                "reside in a feature layer", type -> Location.of(type).isPresent());
    }

    private static ArchCondition<JavaClass> onlyUseOtherFeaturesThroughServiceOrDomain() {
        return new ArchCondition<>("only use other features through their service or domain packages") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Location from = Location.of(origin).orElseThrow();
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    Location.of(dependency.getTargetClass().getBaseComponentType())
                            .filter(to -> !to.feature().equals(from.feature()))
                            .filter(to -> to.layer().equals("controller")
                                    || to.layer().equals("repository"))
                            .ifPresent(to -> events.add(SimpleConditionEvent.violated(
                                    dependency,
                                    dependency.getDescription() + " crosses into another feature's " + to.layer())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notUseFloatingPointTypes() {
        return new ArchCondition<>("not use double or float in fields, parameters or return types") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                for (JavaField field : type.getFields()) {
                    if (isFloatingPoint(field.getRawType())) {
                        events.add(SimpleConditionEvent.violated(field, field.getFullName() + " is floating point"));
                    }
                }
                for (JavaCodeUnit codeUnit : type.getCodeUnits()) {
                    boolean floatingParameter =
                            codeUnit.getRawParameterTypes().stream().anyMatch(ArchitectureRules::isFloatingPoint);
                    if (floatingParameter || isFloatingPoint(codeUnit.getRawReturnType())) {
                        events.add(SimpleConditionEvent.violated(
                                codeUnit, codeUnit.getFullName() + " uses a floating point type"));
                    }
                }
            }
        };
    }

    private static boolean isFloatingPoint(JavaClass type) {
        return FLOATING_POINT_TYPES.contains(type.getBaseComponentType().getName());
    }
}
