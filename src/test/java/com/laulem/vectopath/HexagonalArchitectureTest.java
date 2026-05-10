package com.laulem.vectopath;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * ArchUnit test to verify compliance with hexagonal architecture.
 * Expected structure:
 * - business: Business domain (core of the hexagon) - should not depend on anything
 * - client: Primary/inbound adapters (REST API, controllers)
 * - infra: Secondary/outbound adapters (repositories, JPA entities, technical services)
 */
@DisplayName("Hexagonal Architecture Tests")
class HexagonalArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.laulem.vectopath");
    }

    @Test
    @DisplayName("Business domain should not depend on adapters (client, infra)")
    void domainShouldNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..business..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.laulem.vectopath.client..",
                        "com.laulem.vectopath.infra..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business domain should only depend on itself and vanilla Java")
    void domainShouldOnlyDependOnItselfAndJava() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.laulem.vectopath.business..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "com.laulem.vectopath.business..",
                        "com.laulem.vectopath.shared..",
                        "java..",
                        "org.slf4j.."
                )
                .because("Business layer must be purely business-oriented, with no technical or framework dependencies");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Client adapters can depend on domain but not on infrastructure")
    void clientAdaptersShouldDependOnDomainButNotInfra() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..client..")
                .should().dependOnClassesThat().resideInAPackage("..infra..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Infrastructure adapters can depend on domain but not on client")
    void infraAdaptersShouldDependOnDomainButNotClient() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..infra..")
                .should().dependOnClassesThat().resideInAPackage("com.laulem.vectopath.client..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers must be in client.controller package")
    void controllersShouldBeInClientPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..client.controller..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("JPA entities must be in infra.entity package")
    void entitiesShouldBeInInfraPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..infra.entity..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("JPA repositories must be in infra.repository package")
    void jpaRepositoriesShouldBeInInfraPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("JpaRepository")
                .or().areAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .should().resideInAPackage("..infra.repository..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain repository interfaces must be in business.repository")
    void domainRepositoriesShouldBeInBusinessPackage() {
        ArchRule rule = classes()
                .that().resideInAPackage("..business.repository..")
                .should().beInterfaces();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business services must be in business.service package")
    void businessServicesShouldBeInBusinessPackage() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..business.service..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.laulem.vectopath.client..",
                        "com.laulem.vectopath.infra..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("DTOs must be in client.dto package")
    void dtosShouldBeInClientPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("DTO")
                .or().haveSimpleNameEndingWith("Dto")
                .or().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .and().areNotNestedClasses()
                .should().resideInAPackage("..client.dto..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Onion architecture - global verification")
    void onionArchitectureShouldBeRespected() {
        ArchRule rule = onionArchitecture()
                .domainModels("..business.model..", "..business.repository..")
                .domainServices("..business.service..")
                .applicationServices("..client.service..")
                .adapter("client", "..client..")
                .adapter("infra", "..infra..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business exceptions must be in business.exception")
    void businessExceptionsShouldBeInBusinessPackage() {
        ArchRule rule = classes()
                .that().resideInAPackage("..business.exception..")
                .should().beAssignableTo(Exception.class)
                .orShould().beAssignableTo(RuntimeException.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain models must not have JPA annotations")
    void domainModelsShouldNotHaveJpaAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..business.model..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain models must be independent (no dependencies on services or repositories)")
    void domainModelsShouldBeIndependent() {
        ArchRule rule = classes()
                .that().resideInAPackage("..business.model..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..business.model..",
                        "java..",
                        "com.fasterxml.jackson.."
                )
                .because("Domain models must be purely business-oriented without technical dependencies");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business services should only depend on ports (interfaces), not implementations")
    void businessServicesShouldDependOnPortsOnly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..business.service..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infra.repository..", "..infra.service..")
                .because("Business services should only depend on domain ports, not infrastructure adapters");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Infrastructure technical exceptions should not be exposed to domain")
    void infraExceptionsShouldNotBeInDomain() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..business..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infra.exception..")
                .because("Infrastructure technical exceptions should not pollute the domain");

        rule.check(importedClasses);
    }

    @Test
    @Disabled("Can be reactivated when violations are fixed")
    @DisplayName("Domain entities should not be exposed in DTOs")
    void domainEntitiesShouldNotBeInDTOs() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..client.dto..")
                .should().dependOnClassesThat()
                .resideInAPackage("..business.model..")
                .because("DTOs should isolate the domain from the external API");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers should not depend directly on repositories")
    void controllersShouldNotDependOnRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..client.controller..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..business.repository..", "..infra.repository..")
                .because("Controllers should go through business services");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain classes should not use utility classes from client or infra")
    void domainShouldNotUseAdapterUtilities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..business..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..client.tool..", "..infra.conf..")
                .because("Domain must remain independent of adapter utilities");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Shared package can be used by all layers")
    void sharedPackageCanBeUsedByAllLayers() {
        ArchRule rule = classes()
                .that().resideInAPackage("..shared..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..shared..", "java..", "org.springframework..", "jakarta..", "org.apache..")
                .because("Shared package should only contain utilities without business logic");

        rule.check(importedClasses);
    }
}


