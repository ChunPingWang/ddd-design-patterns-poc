package com.automfg.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.automfg", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
            noClasses()
                    .that().resideInAnyPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jpa =
            noClasses()
                    .that().resideInAnyPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_spring =
            noClasses()
                    .that().resideInAnyPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_jpa =
            noClasses()
                    .that().resideInAnyPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAnyPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..infrastructure..");

    // --- CQRS Rules ---

    @ArchTest
    static final ArchRule query_use_cases_must_not_publish_domain_events =
            noClasses()
                    .that().implement(com.automfg.shared.application.QueryUseCase.class)
                    .should().dependOnClassesThat()
                    .areAssignableTo(com.automfg.shared.domain.DomainEventPublisher.class)
                    .as("CQRS: Query use cases must not publish domain events (queries are side-effect-free)");

    @ArchTest
    static final ArchRule command_use_cases_must_not_also_be_queries =
            noClasses()
                    .that().implement(com.automfg.shared.application.CommandUseCase.class)
                    .should().implement(com.automfg.shared.application.QueryUseCase.class)
                    .as("CQRS: A use case must be either a Command or a Query, never both");
}
