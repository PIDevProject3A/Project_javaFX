-- ===================================================
-- BLEDNA — Seed Data
-- Run this AFTER importing bledna.sql in phpMyAdmin
-- ===================================================

USE `bledna`;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `prevue_collection`;
TRUNCATE TABLE `waste_collection`;
TRUNCATE TABLE `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------------------------------------
-- Static User (collector_id = 1 used by the JavaFX app)
-- -------------------------------------------------------
INSERT INTO `users`
    (`id`, `first_name`, `last_name`, `email`, `password`, `user_type`,
     `gps_location`, `created_at`, `updated_at`, `registration_blocked`)
VALUES
    (1, 'Azmi', 'Bledna', 'azmi.bledna@gmail.com', 'bledna2026',
     'COLLECTOR', '36.7372,3.0868', NOW(), NOW(), 0);

-- -------------------------------------------------------
-- Sample Waste Collections
-- -------------------------------------------------------
INSERT INTO `waste_collection`
    (`collector_id`, `location_name`, `waste_type`, `quantity`,
     `collection_date`, `gps_location`, `status`, `description`,
     `created_at`, `updated_at`, `unit`)
VALUES
    (1, 'Alger Centre', 'PLASTIC', 120.5, '2026-04-01 08:00:00',
     '36.7372,3.0868', 'COMPLETED', 'Collecte plastique zone résidentielle', NOW(), NOW(), 'kg'),
    (1, 'Bab El Oued', 'ORGANIC', 85.0, '2026-04-03 09:30:00',
     '36.7750,3.0553', 'COMPLETED', 'Déchets organiques marché', NOW(), NOW(), 'kg'),
    (1, 'Hussein Dey', 'METAL', 45.2, '2026-04-05 07:00:00',
     '36.7295,3.1047', 'IN_PROGRESS', 'Ferraille zone industrielle', NOW(), NOW(), 'kg'),
    (1, 'El Harrach', 'ELECTRONIC', 30.0, '2026-04-07 10:00:00',
     '36.7120,3.1300', 'PENDING', 'Équipements électroniques usagés', NOW(), NOW(), 'kg'),
    (1, 'Kouba', 'GLASS', 60.0, '2026-04-09 08:30:00',
     '36.7450,3.1100', 'PENDING', 'Verre ménager collecte hebdomadaire', NOW(), NOW(), 'kg');

-- -------------------------------------------------------
-- Sample Prévue Collections
-- -------------------------------------------------------
INSERT INTO `prevue_collection`
    (`type_collection`, `quantite`, `statut`, `waste_collection_id`)
VALUES
    ('Recyclage Plastique Ménager', 200.0, 'PENDING', 1),
    ('Compostage Organique', 150.0, 'IN_PROGRESS', 2),
    ('Récupération Ferraille', 80.0, 'PENDING', 3),
    ('Collecte DEEE', 50.0, 'PENDING', 4),
    ('Collecte Verre Bouteilles', 100.0, 'PENDING', 5);
