CREATE DATABASE IF NOT EXISTS pidevjava;
USE pidevjava;

CREATE TABLE IF NOT EXISTS admin_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_admin_accounts_email (email)
);

CREATE TABLE IF NOT EXISTS event_manager (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_event_manager_email (email)
);

CREATE TABLE IF NOT EXISTS finance_manager (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_finance_manager_email (email)
);

CREATE TABLE IF NOT EXISTS face_id_profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    face_subject VARCHAR(80) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_face_id_profiles_email (email),
    UNIQUE KEY uq_face_id_profiles_subject (face_subject)
);

-- Default admin account is created by the application with a BCrypt-hashed password.
