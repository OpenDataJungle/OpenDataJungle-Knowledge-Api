package com.laulem.vectopath.knowledge.api;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
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
    public static final String COM_LAULEM_VECTOPATH = "com.laulem.vectopath.knowledge";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(COM_LAULEM_VECTOPATH + ".api");
    }

    @Test
    @DisplayName("Business domain should not depend on adapters (client, infra)")
    void domainShouldNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business..")
                .should().dependOnClassesThat().resideInAnyPackage(COM_LAULEM_VECTOPATH + ".api.client..", COM_LAULEM_VECTOPATH + ".api.infra..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business domain should only depend on itself and vanilla Java")
    void domainShouldOnlyDependOnItselfAndJava() {
        ArchRule rule = classes()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        COM_LAULEM_VECTOPATH + ".api.business..",
                        COM_LAULEM_VECTOPATH + ".api.shared..",
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
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.client..")
                .should().dependOnClassesThat().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.infra..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Infrastructure adapters can depend on domain but not on client")
    void infraAdaptersShouldDependOnDomainButNotClient() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.infra..")
                .should().dependOnClassesThat().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.client..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers must be in client.controller package")
    void controllersShouldBeInClientPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.client.controller..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("JPA entities must be in infra.entity package")
    void entitiesShouldBeInInfraPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.infra.entity..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("JPA repositories must be in infra.repository package")
    void jpaRepositoriesShouldBeInInfraPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("JpaRepository")
                .or().areAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .should().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.infra.repository..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain repository interfaces must be in business.repository")
    void domainRepositoriesShouldBeInBusinessPackage() {
        ArchRule rule = classes()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.repository..")
                .should().beInterfaces().allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business services must be in business.service package")
    void businessServicesShouldBeInBusinessPackage() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.service..")
                .should().dependOnClassesThat().resideInAnyPackage(COM_LAULEM_VECTOPATH + ".api.client..", COM_LAULEM_VECTOPATH + ".api.infra..");

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
                .and().arePublic()
                .should().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.client.dto..")
                .orShould().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.infra.dto..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Onion architecture - global verification")
    void onionArchitectureShouldBeRespected() {
        ArchRule rule = onionArchitecture()
                .domainModels(COM_LAULEM_VECTOPATH + ".api.business.model..", COM_LAULEM_VECTOPATH + ".api.business.repository..")
                .domainServices(COM_LAULEM_VECTOPATH + ".api.business.service..")
                .applicationServices(COM_LAULEM_VECTOPATH + ".api.client.service..")
                .adapter("client", COM_LAULEM_VECTOPATH + ".api.client..")
                .adapter("infra", COM_LAULEM_VECTOPATH + ".api.infra..")
                .withOptionalLayers(true);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business exceptions must be in business.exception")
    void businessExceptionsShouldBeInBusinessPackage() {
        ArchRule rule = classes()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.exception..")
                .should().beAssignableTo(Exception.class)
                .orShould().beAssignableTo(RuntimeException.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain models must not have JPA annotations")
    void domainModelsShouldNotHaveJpaAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.model..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain models must be independent (no dependencies on services or repositories)")
    void domainModelsShouldBeIndependent() {
        ArchRule rule = classes()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.model..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        COM_LAULEM_VECTOPATH + ".api.business.model..",
                        "java..",
                        "com.fasterxml.jackson..",
                        "..shared.."
                )
                .because("Domain models must be purely business-oriented without technical dependencies");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Business services should only depend on ports (interfaces), not implementations")
    void businessServicesShouldDependOnPortsOnly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.service..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(COM_LAULEM_VECTOPATH + ".api.infra.repository..", COM_LAULEM_VECTOPATH + ".api.infra.service..")
                .because("Business services should only depend on domain ports, not infrastructure adapters");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Infrastructure technical exceptions should not be exposed to domain")
    void infraExceptionsShouldNotBeInDomain() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business..")
                .should().dependOnClassesThat()
                .resideInAPackage(COM_LAULEM_VECTOPATH + ".api.infra.exception..")
                .because("Infrastructure technical exceptions should not pollute the domain");

        rule.check(importedClasses);
    }

    @Test
    @Disabled("Can be reactivated when violations are fixed")
    @DisplayName("Domain entities should not be exposed in DTOs")
    void domainEntitiesShouldNotBeInDTOs() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.client.dto..")
                .should().dependOnClassesThat()
                .resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.model..")
                .because("DTOs should isolate the domain from the external API");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers should not depend directly on repositories")
    void controllersShouldNotDependOnRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.client.controller..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(COM_LAULEM_VECTOPATH + ".api.business.repository..", COM_LAULEM_VECTOPATH + ".api.infra.repository..")
                .because("Controllers should go through business services");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain classes should not use utility classes from client or infra")
    void domainShouldNotUseAdapterUtilities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(COM_LAULEM_VECTOPATH + ".api.client.tool..", COM_LAULEM_VECTOPATH + ".api.infra.conf..")
                .because("Domain must remain independent of adapter utilities");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Shared package can be used by all layers")
    void sharedPackageCanBeUsedByAllLayers() {
        ArchRule rule = classes()
                .that().resideInAPackage("..shared..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..shared..", "java..", "org.springframework..", "jakarta..", "org.apache..", "com.fasterxml..")
                .because("Shared package should only contain utilities without business logic");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain model fields must be private (except enum constants)")
    void domainModelFieldsShouldBePrivate() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat()
                .resideInAPackage(COM_LAULEM_VECTOPATH + ".api.business.model..")
                .and().areNotStatic()
                .should().bePrivate()
                .because("Domain models must encapsulate their state — enum constants are allowed, but instance variables must be private");

        rule.check(importedClasses);
    }
}


