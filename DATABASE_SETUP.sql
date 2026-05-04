-- =====================================================
-- PIDEVJAVA DATABASE SETUP
-- Complete SQL Script with All Tables
-- =====================================================

-- Create Database
CREATE DATABASE IF NOT EXISTS pidevjava CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pidevjava;

-- =====================================================
-- 1. ADMIN ACCOUNTS (Admin Users Management)
-- =====================================================
CREATE TABLE IF NOT EXISTS admin_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_admin_accounts_email (email),
    INDEX idx_email (email)
);

-- =====================================================
-- 2. EVENT MANAGER (Event Manager Users)
-- =====================================================
CREATE TABLE IF NOT EXISTS event_manager (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_event_manager_email (email),
    INDEX idx_email (email)
);

-- =====================================================
-- 3. FINANCE MANAGER (Finance Manager Users)
-- =====================================================
CREATE TABLE IF NOT EXISTS finance_manager (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_finance_manager_email (email),
    INDEX idx_email (email)
);

-- =====================================================
-- 4. USERS (Collector, Buyer, Donator Users)
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_type ENUM('Collector', 'Buyer', 'Donator') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_admin VARCHAR(150) NULL,
    UNIQUE KEY uq_users_email (email),
    INDEX idx_email (email),
    INDEX idx_user_type (user_type)
);

-- =====================================================
-- 5. LOGIN HISTORY (User Activity Tracking)
-- =====================================================
CREATE TABLE IF NOT EXISTS login_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
);

-- =====================================================
-- 6. EMAIL LOGS (Email Sending Tracking)
-- =====================================================
CREATE TABLE IF NOT EXISTS email_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(150) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    subject VARCHAR(255),
    status ENUM('sent', 'failed') NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recipient (recipient),
    INDEX idx_sent_at (sent_at),
    INDEX idx_status (status)
);

-- =====================================================
-- 7. FACE ID PROFILES (Face Recognition)
-- =====================================================
CREATE TABLE IF NOT EXISTS face_id_profiles (
    email VARCHAR(150) PRIMARY KEY,
    face_subject VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_is_enabled (is_enabled)
);

-- =====================================================
-- 8. EVENTS (Event Management)
-- =====================================================
CREATE TABLE IF NOT EXISTS events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    eventDate TIMESTAMP,
    location VARCHAR(150),
    price DOUBLE,
    payment_type VARCHAR(50),
    event_type VARCHAR(50),
    maxPlaces INT,
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_eventDate (eventDate)
);

-- =====================================================
-- 9. REGISTRATIONS (Event Registrations & Payments)
-- =====================================================
CREATE TABLE IF NOT EXISTS registrations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    event_id INT NOT NULL,
    event_name VARCHAR(150),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(150),
    registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DOUBLE,
    payment_method VARCHAR(50),
    status VARCHAR(50),
    budget DOUBLE,
    isPaid BOOLEAN DEFAULT FALSE,
    paymentDate TIMESTAMP NULL,
    stripe_session_id VARCHAR(255) NULL,
    stripe_payment_intent_id VARCHAR(255) NULL,
    payment_status VARCHAR(30) NULL DEFAULT 'PENDING',
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_event_id (event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_payment_status (payment_status)
);

-- =====================================================
-- 10. FORUM - CATEGORIES (Topic Categories)
-- =====================================================
CREATE TABLE IF NOT EXISTS topic_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);

-- =====================================================
-- 11. FORUM - TOPICS (Forum Discussion Topics)
-- =====================================================
CREATE TABLE IF NOT EXISTS topics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    category_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    reply_count INT DEFAULT 0,
    image_path VARCHAR(255) NULL,
    FOREIGN KEY (category_id) REFERENCES topic_categories(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_category_id (category_id),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- 12. FORUM - REPONSES (Forum Replies/Responses)
-- =====================================================
CREATE TABLE IF NOT EXISTS reponses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    topic_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
    INDEX idx_topic_id (topic_id),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- 13. FORUM - NOTIFICATIONS (User Notifications)
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    read_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_read_status (read_status),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Add additional indexes for common queries
ALTER TABLE events ADD INDEX idx_name (name);
ALTER TABLE events ADD INDEX idx_location (location);
ALTER TABLE registrations ADD INDEX idx_registration_date (registration_date);
ALTER TABLE topics ADD INDEX idx_title (title);
ALTER TABLE topics ADD INDEX idx_like_count (like_count);
ALTER TABLE reponses ADD INDEX idx_like_count (like_count);

-- =====================================================
-- END OF SCHEMA
-- =====================================================
-- Note: Default admin account will be created automatically by the application
-- with BCrypt hashed password during first initialization.
