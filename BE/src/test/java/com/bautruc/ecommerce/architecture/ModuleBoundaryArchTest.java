package com.bautruc.ecommerce.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.bautruc.ecommerce", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryArchTest {
    @ArchTest
    static final ArchRule paymentDoesNotDependOnForeignInfrastructure = noClasses()
            .that().resideInAPackage("..payment..")
            .should().dependOnClassesThat().resideInAnyPackage("..inventory.infrastructure..", "..order.infrastructure..");

    @ArchTest
    static final ArchRule orderDoesNotDependOnPaymentInfrastructure = noClasses()
            .that().resideInAPackage("..order..")
            .should().dependOnClassesThat().resideInAPackage("..payment.infrastructure..");

    @ArchTest
    static final ArchRule commerceDoesNotDependOnForeignInfrastructure = noClasses()
            .that().resideInAPackage("..commerce..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..catalog.infrastructure..",
                    "..cart.infrastructure..",
                    "..inventory.infrastructure..",
                    "..identity.infrastructure..",
                    "..order.infrastructure..",
                    "..payment.infrastructure..",
                    "..notification.infrastructure..",
                    "..reporting.infrastructure..",
                    "..workshop.infrastructure..",
                    "..support.infrastructure.."
            );

    @ArchTest
    static final ArchRule workshopDoesNotDependOnOtherBusinessInfrastructure = noClasses()
            .that().resideInAPackage("..workshop..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..catalog.infrastructure..",
                    "..cart.infrastructure..",
                    "..inventory.infrastructure..",
                    "..identity.infrastructure..",
                    "..order.infrastructure..",
                    "..payment.infrastructure..",
                    "..notification.infrastructure..",
                    "..reporting.infrastructure..",
                    "..support.infrastructure.."
            );

    @ArchTest
    static final ArchRule supportDoesNotDependOnOtherBusinessInfrastructure = noClasses()
            .that().resideInAPackage("..support..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..catalog.infrastructure..",
                    "..cart.infrastructure..",
                    "..inventory.infrastructure..",
                    "..identity.infrastructure..",
                    "..order.infrastructure..",
                    "..payment.infrastructure..",
                    "..notification.infrastructure..",
                    "..reporting.infrastructure..",
                    "..workshop.infrastructure.."
            );
}
