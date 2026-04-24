-- Table inscriptions : prénom/nom + date optionnelle par défaut MySQL.
-- Si la table existe déjà SANS registration_date ou first_name : exécutez migration_registrations.sql

DROP TABLE IF EXISTS registrations;

CREATE TABLE registrations (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    user_id             INT             NOT NULL DEFAULT 1,
    event_id            INT             NOT NULL,
    first_name          VARCHAR(80)     NOT NULL,
    last_name           VARCHAR(80)     NOT NULL,
    registration_date   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount              DOUBLE          NOT NULL DEFAULT 0,
    payment_method      VARCHAR(30)     NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'REGISTERED',
    KEY idx_reg_event (event_id),
    KEY idx_reg_user (user_id),
    CONSTRAINT fk_registrations_event
        FOREIGN KEY (event_id) REFERENCES events (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
