-- À exécuter sur une base existante (une commande à la fois ; ignorez si « Duplicate column »).

ALTER TABLE registrations ADD COLUMN registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE registrations ADD COLUMN first_name VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE registrations ADD COLUMN last_name VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE registrations ADD COLUMN email VARCHAR(180) NULL;
ALTER TABLE registrations ADD COLUMN stripe_session_id VARCHAR(255) NULL;
ALTER TABLE registrations ADD COLUMN stripe_payment_intent_id VARCHAR(255) NULL;
ALTER TABLE registrations ADD COLUMN payment_status VARCHAR(30) NULL DEFAULT 'PENDING';

-- Supprimer l'ancienne contrainte « un seul enregistrement par user + event » si elle existe :
-- Essayez l'un des noms suivants selon votre table :
-- ALTER TABLE registrations DROP INDEX uk_user_event;
-- ALTER TABLE registrations DROP INDEX uk_participant_event;
