# Database Configuration Update Summary

## ✅ Changes Made

### 1. Java Database Configuration Files Updated
All Java files have been updated to use **`pidevjava`** database instead of `bledna`:

#### Files Updated:
- ✅ `src/main/java/com/esprit/utils/MyDataBase.java`
  - Changed: `DB_NAME = "bledna"` → `DB_NAME = "pidevjava"`
  
- ✅ `src/main/java/org/example/utils/MyDataBase.java`
  - Changed: `URL = "jdbc:mysql://localhost:3306/bledna"` → `URL = "jdbc:mysql://localhost:3306/pidevjava"`

---

## 📋 SQL Commands for Database Setup

### Option 1: Execute Complete Schema (Recommended)

Run the file: `DATABASE_SETUP.sql` in phpMyAdmin

This file contains:
1. **Create Database**: `pidevjava`
2. **Admin Management Tables**:
   - `admin_accounts` - Admin users
   - `event_manager` - Event manager users
   - `finance_manager` - Finance manager users
3. **App Users Table**:
   - `users` - Collector, Buyer, Donator users
4. **Activity Tracking**:
   - `login_history` - User login/logout tracking
   - `email_logs` - Email sending tracking
5. **Face Recognition**:
   - `face_id_profiles` - Face ID enrollment data
6. **Events & Registrations**:
   - `events` - Event definitions
   - `registrations` - Event registrations with payment info
7. **Forum Tables**:
   - `topic_categories` - Forum categories
   - `topics` - Discussion topics
   - `reponses` - Topic replies
   - `notifications` - User notifications

---

### Option 2: Manual Creation in phpMyAdmin

#### Step 1: Create Database
```sql
CREATE DATABASE IF NOT EXISTS pidevjava CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pidevjava;
```

#### Step 2: Create Admin Accounts Table
```sql
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
```

#### Step 3: Create Event Manager Table
```sql
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
```

#### Step 4: Create Finance Manager Table
```sql
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
```

#### Step 5: Create Users Table (Collector/Buyer/Donator)
```sql
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
```

#### Step 6: Create Login History Table
```sql
CREATE TABLE IF NOT EXISTS login_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
);
```

#### Step 7: Create Email Logs Table
```sql
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
```

#### Step 8: Create Face ID Profiles Table
```sql
CREATE TABLE IF NOT EXISTS face_id_profiles (
    email VARCHAR(150) PRIMARY KEY,
    face_subject VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_is_enabled (is_enabled)
);
```

#### Step 9: Create Events Table
```sql
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
```

#### Step 10: Create Registrations Table
```sql
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
```

#### Step 11: Create Forum - Topic Categories Table
```sql
CREATE TABLE IF NOT EXISTS topic_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);
```

#### Step 12: Create Forum - Topics Table
```sql
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
```

#### Step 13: Create Forum - Responses Table
```sql
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
```

#### Step 14: Create Notifications Table
```sql
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
```

---

## 📝 Instructions for phpMyAdmin

1. **Open phpMyAdmin** in your browser
2. **Method 1 - Easy (Recommended)**:
   - Click on "Import" tab
   - Select the file `DATABASE_SETUP.sql` from `/database/` folder
   - Click "Go"

3. **Method 2 - Manual**:
   - Copy SQL commands from above
   - Paste into phpMyAdmin SQL tab
   - Execute each command one by one

---

## 🔍 Verification

After creating tables, verify they exist:

```sql
USE pidevjava;
SHOW TABLES;
```

Expected output should show all 14 tables:
- admin_accounts
- event_manager
- finance_manager
- users
- login_history
- email_logs
- face_id_profiles
- events
- registrations
- topic_categories
- topics
- reponses
- notifications

---

## ⚠️ Notes

- **Database Connection**: The application will connect to `pidevjava` automatically
- **Default Admin**: First login will automatically create a default admin account with:
  - Email: `admin@bladna.tn`
  - Password: `123456789` (auto-hashed with BCrypt)
- **No Data Loss**: All functionality is preserved, just migrated to new database name
- **Character Set**: UTF8MB4 for full emoji and special character support

---

## ✅ Completed

- ✅ Database name updated to `pidevjava` in all Java files
- ✅ SQL commands provided for all required tables
- ✅ Ready for database setup in phpMyAdmin
