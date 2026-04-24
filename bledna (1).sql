-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : jeu. 09 avr. 2026 à 21:40
-- Version du serveur : 10.4.28-MariaDB
-- Version de PHP : 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `bledna`
--

-- --------------------------------------------------------

--
-- Structure de la table `prevue_collection`
--

CREATE TABLE `prevue_collection` (
  `id` int(11) NOT NULL,
  `type_collection` varchar(255) NOT NULL,
  `quantite` double NOT NULL,
  `statut` varchar(50) NOT NULL DEFAULT 'PENDING',
  `waste_collection_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `prevue_collection`
--

INSERT INTO `prevue_collection` (`id`, `type_collection`, `quantite`, `statut`, `waste_collection_id`) VALUES
(1, 'Recyclage Plastique M├®nager', 200, 'PENDING', 1),
(2, 'Compostage Organique', 150, 'IN_PROGRESS', 2),
(3, 'R├®cup├®ration Ferraille', 80, 'PENDING', 3),
(4, 'Collecte DEEE', 50, 'PENDING', 4),
(5, 'Collecte Verre Bouteilles', 100, 'PENDING', 5),
(7, 'ddddddddd', 150, 'PENDING', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `google_id` varchar(255) DEFAULT NULL,
  `user_type` varchar(50) NOT NULL,
  `gps_location` varchar(255) DEFAULT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `payment_account` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `reset_token` varchar(100) DEFAULT NULL,
  `reset_token_expires_at` datetime DEFAULT NULL,
  `azure_face_person_id` varchar(255) DEFAULT NULL,
  `registration_blocked` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`id`, `first_name`, `last_name`, `email`, `password`, `google_id`, `user_type`, `gps_location`, `profile_picture`, `payment_account`, `created_at`, `updated_at`, `reset_token`, `reset_token_expires_at`, `azure_face_person_id`, `registration_blocked`) VALUES
(1, 'Azmi', 'Bledna', 'azmi.bledna@gmail.com', 'bledna2026', NULL, 'COLLECTOR', '36.7372,3.0868', NULL, NULL, '2026-04-09 20:22:34', '2026-04-09 20:22:34', NULL, NULL, NULL, 0);

-- --------------------------------------------------------

--
-- Structure de la table `waste_collection`
--

CREATE TABLE `waste_collection` (
  `id` int(11) NOT NULL,
  `collector_id` int(11) DEFAULT NULL,
  `location_name` varchar(255) NOT NULL,
  `waste_type` varchar(50) NOT NULL,
  `quantity` double NOT NULL,
  `collection_date` datetime NOT NULL,
  `gps_location` varchar(255) DEFAULT NULL,
  `status` varchar(50) DEFAULT 'PENDING',
  `description` text DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `unit` varchar(10) DEFAULT 'kg'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `waste_collection`
--

INSERT INTO `waste_collection` (`id`, `collector_id`, `location_name`, `waste_type`, `quantity`, `collection_date`, `gps_location`, `status`, `description`, `created_at`, `updated_at`, `unit`) VALUES
(1, 1, 'Alger Centre', 'PLASTIC', 120.5, '2026-04-01 08:00:00', '36.7372,3.0868', 'COMPLETED', 'Collecte plastique zone r├®sidentielle', '2026-04-09 20:22:34', '2026-04-09 20:22:34', 'kg'),
(2, 1, 'Bab El Oued', 'ORGANIC', 85, '2026-04-03 09:30:00', '36.7750,3.0553', 'COMPLETED', 'D├®chets organiques march├®', '2026-04-09 20:22:34', '2026-04-09 20:22:34', 'kg'),
(3, 1, 'Hussein Dey', 'METAL', 45.2, '2026-04-05 07:00:00', '36.7295,3.1047', 'IN_PROGRESS', 'Ferraille zone industrielle', '2026-04-09 20:22:34', '2026-04-09 20:22:34', 'kg'),
(4, 1, 'El Harrach', 'ELECTRONIC', 30, '2026-04-07 10:00:00', '36.7120,3.1300', 'PENDING', '├ëquipements ├®lectroniques usag├®s', '2026-04-09 20:22:34', '2026-04-09 20:22:34', 'kg'),
(5, 1, 'Kouba', 'GLASS', 60, '2026-04-09 08:30:00', '36.7450,3.1100', 'PENDING', 'Verre m├®nager collecte hebdomadaire', '2026-04-09 20:22:34', '2026-04-09 20:22:34', 'kg'),
(7, 1, 'd', 'PLASTIC', 10.5, '2026-04-09 07:00:00', '36.55,32.55', 'PENDING', 'dddddddddddddddddddd', '2026-04-09 20:30:11', '2026-04-09 20:30:11', 'kg');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `prevue_collection`
--
ALTER TABLE `prevue_collection`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_waste_collection` (`waste_collection_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Index pour la table `waste_collection`
--
ALTER TABLE `waste_collection`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_collector` (`collector_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `prevue_collection`
--
ALTER TABLE `prevue_collection`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `waste_collection`
--
ALTER TABLE `waste_collection`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `prevue_collection`
--
ALTER TABLE `prevue_collection`
  ADD CONSTRAINT `fk_waste_collection` FOREIGN KEY (`waste_collection_id`) REFERENCES `waste_collection` (`id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `waste_collection`
--
ALTER TABLE `waste_collection`
  ADD CONSTRAINT `fk_collector` FOREIGN KEY (`collector_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
