CREATE DATABASE IF NOT EXISTS bladna;
USE bladna;

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

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_type ENUM('Collector', 'Buyer', 'Donator') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_admin VARCHAR(150) NULL,
    UNIQUE KEY uq_users_email (email)
);

-- Default admin account is created by the application with a BCrypt-hashed password.

CREATE TABLE IF NOT EXISTS login_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS email_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(150) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    subject VARCHAR(255),
    status ENUM('sent', 'failed') NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
-- 14. RECYCLING BUYERS (Waste Buyers)
-- =====================================================
CREATE TABLE IF NOT EXISTS recycling_buyers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_name VARCHAR(150) NOT NULL,
    recycling_type VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    latitude DOUBLE,
    longitude DOUBLE,
    contact_phone VARCHAR(50),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_city (city),
    INDEX idx_status (status)
);

-- =====================================================
-- 15. DONATIONS (Monetary & Goods Contributions)
-- =====================================================
CREATE TABLE IF NOT EXISTS donations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    donor_name VARCHAR(120) NOT NULL,
    donation_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 3) DEFAULT 0.000,
    payment_method VARCHAR(50) NOT NULL,
    donation_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT NULL,
    tree_count INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_donor_name (donor_name),
    INDEX idx_donation_type (donation_type)
);

-- =====================================================
-- 16. ECO TRANSACTIONS (Financial Impact Tracking)
-- =====================================================
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reference_code VARCHAR(40) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    source_type VARCHAR(100) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 3) NOT NULL,
    impact_unit VARCHAR(50) NOT NULL,
    impact_quantity INT NULL,
    transaction_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_transactions_ref (reference_code),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_status (status)
);

-- =====================================================
-- 17. WASTE COLLECTION (Collection Management)
-- =====================================================
CREATE TABLE IF NOT EXISTS waste_collection (
    id INT AUTO_INCREMENT PRIMARY KEY,
    collector_id INT DEFAULT 1,
    location_name VARCHAR(255),
    waste_type VARCHAR(100),
    quantity DOUBLE,
    collection_date DATETIME,
    gps_location VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    unit VARCHAR(20) DEFAULT 'kg',
    image_path VARCHAR(255),
    INDEX idx_collector_id (collector_id),
    INDEX idx_status (status),
    INDEX idx_waste_type (waste_type)
);

-- =====================================================
-- 18. PREVUE COLLECTION (Collection Scheduling)
-- =====================================================
CREATE TABLE IF NOT EXISTS prevue_collection (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type_collection VARCHAR(100),
    quantite DOUBLE,
    statut VARCHAR(50) DEFAULT 'PENDING',
    waste_collection_id INT,
    collector_id INT,
    collection_date DATETIME,
    unit VARCHAR(20) DEFAULT 'kg',
    FOREIGN KEY (waste_collection_id) REFERENCES waste_collection(id) ON DELETE SET NULL,
    INDEX idx_collector_id (collector_id),
    INDEX idx_statut (statut)
);
