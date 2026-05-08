package org.example.services;

import org.example.entities.Topic;
import org.example.entities.TopicStatus;
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
 * Tests unitaires / d’intégration JDBC sur {@link ForumServices} (entité {@link Topic}).
 * <p>
 * Prérequis : MySQL accessible (voir {@link org.example.utils.MyDataBase}), base {@code bledna},
 * table {@code topic}.
 * </p>
 * L’ordre des tests est important : ajout → modification → lecture → suppression.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ForumServicesTest {

    private static ForumServices forumServices;
    /** Identifiant du sujet inséré dans {@link #testAjouterTopic()}. */
    private static int idTopicJUnit = -1;
    private static final String PREFIX_TITRE = "TEST_JUNIT_TOPIC_";

    @BeforeAll
    static void setup() {
        forumServices = new ForumServices();
    }

    @Test
    @Order(1)
    void testAjouterTopic() throws SQLException {
        String titreUnique = PREFIX_TITRE + System.nanoTime();
        Topic topic = new Topic(
                titreUnique,
                "Contenu insertion JUnit sans apostrophe",
                TopicStatus.PENDING,
                new Date(),
                null);
        forumServices.ajouter(topic);

        List<Topic> topics = forumServices.afficher();
        assertFalse(topics.isEmpty(), "La liste des sujets ne doit pas être vide après insertion.");

        Topic insere = topics.stream()
                .filter(t -> titreUnique.equals(t.getTitle()))
                .findFirst()
                .orElse(null);
        assertNotNull(insere, "Le sujet inséré doit apparaître dans afficher().");
        idTopicJUnit = insere.getId();
        assertTrue(idTopicJUnit > 0, "L’identifiant généré doit être strictement positif.");
    }

    @Test
    @Order(2)
    void testModifierTopic() throws SQLException {
        assertTrue(idTopicJUnit > 0, "Exécuter d’abord testAjouterTopic.");

        Topic aModifier = new Topic();
        aModifier.setId(idTopicJUnit);
        aModifier.setTitle("TitreModifieJUnit");
        aModifier.setContent("Contenu modifie JUnit");
        aModifier.setStatus(TopicStatus.ACCEPTED);
        aModifier.setUpdated_at(new Date());
        forumServices.modifier(aModifier);

        List<Topic> topics = forumServices.afficher();
        boolean trouve = topics.stream()
                .anyMatch(t -> t.getId() == idTopicJUnit
                        && "TitreModifieJUnit".equals(t.getTitle())
                        && TopicStatus.ACCEPTED.equals(t.getStatus()));
        assertTrue(trouve, "La modification titre / statut doit être visible via afficher().");
    }

    @Test
    @Order(3)
    void testAfficherTopics() throws SQLException {
        assertTrue(idTopicJUnit > 0);
        List<Topic> topics = forumServices.afficher();
        assertNotNull(topics);
        assertTrue(topics.stream().anyMatch(t -> t.getId() == idTopicJUnit),
                "Le sujet de test doit toujours être présent avant suppression.");
    }

    @Test
    @Order(4)
    void testSupprimerTopic() throws SQLException {
        assertTrue(idTopicJUnit > 0);
        forumServices.supprimer(idTopicJUnit);
        List<Topic> topics = forumServices.afficher();
        assertTrue(topics.stream().noneMatch(t -> t.getId() == idTopicJUnit),
                "Après suppression, le sujet ne doit plus apparaître.");
    }
}
