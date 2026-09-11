package tn.sia.b2b.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("tn.sia.b2b");

    @Test
    void controllers_must_not_access_repositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().accessClassesThat().resideInAPackage("..repository..")
                .because("Les contrôleurs ne doivent pas accéder directement aux repositories — passer par un service.");

        rule.check(classes);
    }

    @Test
    void ingestion_must_not_import_business_modules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("tn.sia.b2b.ingestion..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "tn.sia.b2b.catalog..",
                        "tn.sia.b2b.pricing..",
                        "tn.sia.b2b.cart..",
                        "tn.sia.b2b.ordering..",
                        "tn.sia.b2b.outbox.."
                )
                .because("Le module ingestion ne doit connaître que l'enveloppe événementielle et la table brute — aucun module métier.");

        rule.check(classes);
    }

    @Test
    void value_objects_must_not_depend_on_spring_or_jpa() {
        // Les objets valeur partagés (Money, Sku, Quantity) sont de purs objets Java.
        // Les entités JPA (User, Order…) dans les modules métier PEUVENT utiliser jakarta.persistence.
        ArchRule rule = noClasses()
                .that().resideInAPackage("tn.sia.b2b.shared.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence.."
                )
                .because("Les objets valeur du domaine partagé (Money, Sku, Quantity) ne doivent dépendre ni de Spring ni de JPA.");

        rule.check(classes);
    }

    @Test
    void projection_relay_must_not_import_identity_module() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("tn.sia.b2b.projection..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "tn.sia.b2b.identity..",
                        "tn.sia.b2b.catalog..",
                        "tn.sia.b2b.pricing..",
                        "tn.sia.b2b.cart..",
                        "tn.sia.b2b.ordering.."
                )
                .because("Le module projection est une couche technique pure — aucune dépendance vers les modules métier ou identité.");

        rule.check(classes);
    }

    @Test
    void projection_consumer_must_not_import_identity_or_catalog_modules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("tn.sia.b2b.projection.consumer..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "tn.sia.b2b.identity..",
                        "tn.sia.b2b.catalog..",
                        "tn.sia.b2b.pricing..",
                        "tn.sia.b2b.cart..",
                        "tn.sia.b2b.ordering.."
                )
                .because("Le consommateur de projection est une couche technique pure sans connaissance des modules métier.");

        rule.check(classes);
    }
}
