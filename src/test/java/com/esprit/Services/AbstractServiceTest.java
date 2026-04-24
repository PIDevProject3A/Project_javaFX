package com.esprit.Services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Schéma H2 en mémoire aligné sur les requêtes MySQL de l’application (événements + inscriptions).
 * Même esprit que les tests de type {@code PersonneServiceTest} (JUnit 5, @BeforeEach / @AfterEach).
 */
abstract class AbstractServiceTest {

    static final String JDBC =
            "jdbc:h2:mem:pijava_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE";

    Connection conn;

    void openConnection() throws SQLException {
        conn = DriverManager.getConnection(JDBC);
    }

    void createBaseSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS registrations");
            st.execute("DROP TABLE IF EXISTS events");
            st.execute("""
                    CREATE TABLE events (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(200) NOT NULL,
                        description VARCHAR(500),
                        eventdate TIMESTAMP NOT NULL,
                        location VARCHAR(200),
                        price DOUBLE NOT NULL,
                        payment_type VARCHAR(30) NOT NULL,
                        event_type VARCHAR(30) NOT NULL,
                        maxplaces INT NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE registrations (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL DEFAULT 1,
                        event_id INT NOT NULL,
                        first_name VARCHAR(80) NOT NULL,
                        last_name VARCHAR(80) NOT NULL,
                        registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        amount DOUBLE NOT NULL DEFAULT 0,
                        payment_method VARCHAR(30) NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
                        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    void closeConnection() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
