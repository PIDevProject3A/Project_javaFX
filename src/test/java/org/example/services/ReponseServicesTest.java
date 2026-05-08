package org.example.services;

import org.example.entities.Reponse;
import org.example.entities.Topic;
import org.example.entities.TopicStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests sur {@link ReponseServices} (entité {@link Reponse}).
 * <p>
 * Une ligne {@link Topic} parent est créée dans {@link #setup()} pour respecter la clé étrangère
 * {@code topic_id} vers {@code topic}.
 * </p>
 * Prérequis : MySQL, base {@code bledna}, tables {@code topic} et {@code forum_reponse}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReponseServicesTest {

    private static ReponseServices reponseServices;
    private static ForumServices forumServices;
    /** Sujet créé uniquement pour lier les réponses de test. */
    private static int idTopicParent = -1;
    private static int idReponseJUnit = -1;

    @BeforeAll
    static void setup() throws SQLException {
        reponseServices = new ReponseServices();
        forumServices = new ForumServices();

        String titreParent = "TEST_JUNIT_PARENT_REPONSE_" + System.nanoTime();
        Topic parent = new Topic(
                titreParent,
                "Sujet parent pour tests JUnit reponse",
                TopicStatus.PENDING,
                new Date(),
                null);
        forumServices.ajouter(parent);

        idTopicParent = forumServices.afficher().stream()
                .filter(t -> titreParent.equals(t.getTitle()))
                .mapToInt(Topic::getId)
                .findFirst()
                .orElse(-1);
        assertTrue(idTopicParent > 0, "Le sujet parent doit être inséré avec un id valide.");
    }

    @Test
    @Order(1)
    void testAjouterReponse() throws SQLException {
        Date maintenant = new Date();
        Reponse r = new Reponse("Message JUnit reponse test", idTopicParent, maintenant, null);
        reponseServices.ajouter(r);

        List<Reponse> parTopic = reponseServices.afficherParTopic(idTopicParent);
        assertFalse(parTopic.isEmpty(), "Au moins une réponse doit exister pour ce sujet.");

        Reponse inseree = parTopic.stream()
                .filter(x -> "Message JUnit reponse test".equals(x.getContent()))
                .findFirst()
                .orElse(null);
        assertNotNull(inseree);
        idReponseJUnit = inseree.getId();
        assertTrue(idReponseJUnit > 0);
    }

    @Test
    @Order(2)
    void testModifierReponse() throws SQLException {
        assertTrue(idReponseJUnit > 0);

        List<Reponse> avant = reponseServices.afficherParTopic(idTopicParent);
        Reponse origine = avant.stream()
                .filter(x -> x.getId() == idReponseJUnit)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Réponse introuvable avant modification."));

        Reponse miseAJour = new Reponse();
        miseAJour.setId(idReponseJUnit);
        miseAJour.setContent("Contenu reponse modifie JUnit");
        miseAJour.setTopic_id(idTopicParent);
        miseAJour.setCreated_at(origine.getCreated_at());
        miseAJour.setUpdated_at(new Date());
        reponseServices.modifier(miseAJour);

        List<Reponse> apres = reponseServices.afficherParTopic(idTopicParent);
        assertTrue(apres.stream().anyMatch(x -> x.getId() == idReponseJUnit
                        && "Contenu reponse modifie JUnit".equals(x.getContent())),
                "Le contenu modifié doit apparaître dans afficherParTopic.");
    }

    @Test
    @Order(3)
    void testAfficherReponses() throws SQLException {
        assertTrue(idReponseJUnit > 0);
        List<Reponse> toutes = reponseServices.afficher();
        assertNotNull(toutes);
        assertTrue(toutes.stream().anyMatch(x -> x.getId() == idReponseJUnit),
                "La réponse doit apparaître dans la liste globale afficher().");
    }

    @Test
    @Order(4)
    void testSupprimerReponse() throws SQLException {
        assertTrue(idReponseJUnit > 0);
        reponseServices.supprimer(idReponseJUnit);
        List<Reponse> restantes = reponseServices.afficherParTopic(idTopicParent);
        assertTrue(restantes.stream().noneMatch(x -> x.getId() == idReponseJUnit));
    }

    @AfterAll
    static void nettoyerTopicParent() throws SQLException {
        if (idTopicParent > 0) {
            forumServices.supprimer(idTopicParent);
        }
    }
}
