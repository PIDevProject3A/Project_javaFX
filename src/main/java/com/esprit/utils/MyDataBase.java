package com.esprit.utils;

import com.esprit.entities.AppUser;
import com.esprit.entities.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class MyDataBase {
    private static final MyDataBase INSTANCE = new MyDataBase();
    private static final String DB_NAME = "bledna";
    private static final String JDBC_OPTIONS = "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/?"+ JDBC_OPTIONS;
    private static final String URL = "jdbc:mysql://localhost:3306/" + DB_NAME + "?" + JDBC_OPTIONS;
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private static final String DEFAULT_ADMIN_FIRST_NAME = "Default";
    private static final String DEFAULT_ADMIN_LAST_NAME = "Admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@bladna.tn";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456789";

    private MyDataBase() {
        ensureDatabaseExists();
        ensureTablesExist();
        ensureDefaultAdmin();
    }

    private void ensureDatabaseExists() {
        String sql = "CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection connection = DriverManager.getConnection(SERVER_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to ensure database '" + DB_NAME + "' exists.", e);
        }
    }

    private void ensureTablesExist() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS admin_accounts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100) NOT NULL," +
                    "last_name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(150) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS event_manager (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100) NOT NULL," +
                    "last_name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(150) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS finance_manager (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100) NOT NULL," +
                    "last_name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(150) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            
            statement.execute("CREATE TABLE IF NOT EXISTS admin_accounts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100)," +
                    "last_name VARCHAR(100)," +
                    "email VARCHAR(150) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS event_manager (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100)," +
                    "last_name VARCHAR(100)," +
                    "email VARCHAR(150) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS finance_manager (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100)," +
                    "last_name VARCHAR(100)," +
                    "email VARCHAR(150) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");

            // Login History Table
            statement.execute("CREATE TABLE IF NOT EXISTS login_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "user_role VARCHAR(50) NOT NULL," +
                    "login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "logout_time TIMESTAMP NULL" +
                    ")");

            // Email Logs Table
            statement.execute("CREATE TABLE IF NOT EXISTS email_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "sender VARCHAR(150) NOT NULL," +
                    "recipient VARCHAR(150) NOT NULL," +
                    "subject VARCHAR(255)," +
                    "status ENUM('sent', 'failed') NOT NULL," +
                    "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // Face ID Profiles Table (ensuring it exists as well)
            statement.execute("CREATE TABLE IF NOT EXISTS face_id_profiles (" +
                    "email VARCHAR(150) PRIMARY KEY," +
                    "face_subject VARCHAR(255) NOT NULL," +
                    "is_enabled BOOLEAN DEFAULT TRUE," +
                    "enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // Events Table
            statement.execute("CREATE TABLE IF NOT EXISTS events (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL," +
                    "description TEXT," +
                    "eventDate TIMESTAMP," +
                    "location VARCHAR(150)," +
                    "price DOUBLE," +
                    "payment_type VARCHAR(50)," +
                    "event_type VARCHAR(50)," +
                    "maxPlaces INT," +
                    "status VARCHAR(50) DEFAULT 'OPEN'" +
                    ")");

            // Registrations Table
            statement.execute("CREATE TABLE IF NOT EXISTS registrations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT," +
                    "event_id INT NOT NULL," +
                    "event_name VARCHAR(150)," +
                    "first_name VARCHAR(100)," +
                    "last_name VARCHAR(100)," +
                    "email VARCHAR(150)," +
                    "registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "amount DOUBLE," +
                    "payment_method VARCHAR(50)," +
                    "status VARCHAR(50)," +
                    "budget DOUBLE," +
                    "isPaid BOOLEAN DEFAULT FALSE," +
                    "paymentDate TIMESTAMP NULL," +
                    "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE" +
                    ")");

            // Users Table (Collector / Buyer / Donator)
            statement.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100) NOT NULL," +
                    "last_name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(150) NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "user_type ENUM('Collector', 'Buyer', 'Donator') NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "created_by_admin VARCHAR(150) NULL," +
                    "UNIQUE KEY uq_users_email (email)" +
                    ")");
            // Try to alter the users table to add created_by_admin if it doesn't exist
            try {
                statement.execute("ALTER TABLE users ADD COLUMN created_by_admin VARCHAR(150) NULL");
            } catch (SQLException ignored) {
                // Column might already exist, safe to ignore
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to ensure tables exist.", e);
        }
    }

    public static MyDataBase getInstance() {
        return INSTANCE;
    }

    public synchronized boolean addUser(User user) {
        String email = normalizeEmail(user.getEmail());
        if (findByEmail(email) != null) {
            return false;
        }

        String sql = "INSERT INTO " + getTableName(user.getAdminType()) + " (first_name, last_name, email, password) VALUES (?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setString(3, email);
            statement.setString(4, user.getPasswordHash());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert user in database.", e);
        }
    }

    public synchronized User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = findByEmailInTable(normalizedEmail, "admin_accounts", User.AdminType.ADMIN_ACCOUNT);
        if (user != null) {
            return user;
        }

        user = findByEmailInTable(normalizedEmail, "event_manager", User.AdminType.EVENT_MANAGER);
        if (user != null) {
            return user;
        }

        return findByEmailInTable(normalizedEmail, "finance_manager", User.AdminType.FINANCE_MANAGER);
    }

    public synchronized AppUser findAppUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String sql = "SELECT id, first_name, last_name, email, password, user_type, created_at, created_by_admin FROM users WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapAppUser(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to find app user by email.", e);
        }
    }

    public synchronized boolean addAppUser(AppUser appUser) {
        String email = normalizeEmail(appUser.getEmail());
        // Check uniqueness across ALL tables
        if (findByEmail(email) != null || findAppUserByEmail(email) != null) {
            return false;
        }

        String sql = "INSERT INTO users (first_name, last_name, email, password, user_type, created_by_admin) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appUser.getFirstName());
            statement.setString(2, appUser.getLastName());
            statement.setString(3, email);
            statement.setString(4, appUser.getPasswordHash());
            statement.setString(5, appUser.getUserType().name());
            statement.setString(6, appUser.getCreatedByAdmin());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert app user.", e);
        }
    }

    public synchronized List<AppUser> findAllAppUsers() {
        String sql = "SELECT id, first_name, last_name, email, password, user_type, created_at, created_by_admin FROM users ORDER BY email";
        List<AppUser> users = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                users.add(mapAppUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch all app users.", e);
        }
    }

    public synchronized boolean updateAppUserProfile(String currentEmail,
                                                      String newFirstName,
                                                      String newLastName,
                                                      String newEmail,
                                                      String newPassword,
                                                      AppUser.UserType newUserType) {
        String normalizedCurrentEmail = normalizeEmail(currentEmail);
        String normalizedNewEmail = normalizeEmail(newEmail);

        if (!normalizedCurrentEmail.equals(normalizedNewEmail)) {
            if (findByEmail(normalizedNewEmail) != null || findAppUserByEmail(normalizedNewEmail) != null) {
                return false;
            }
        }

        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, password = ?, user_type = ? WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newFirstName == null ? "" : newFirstName.trim());
            statement.setString(2, newLastName == null ? "" : newLastName.trim());
            statement.setString(3, normalizedNewEmail);
            statement.setString(4, newPassword);
            statement.setString(5, newUserType.name());
            statement.setString(6, normalizedCurrentEmail);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update app user profile.", e);
        }
    }

    public synchronized boolean deleteAppUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String sql = "DELETE FROM users WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            boolean deleted = statement.executeUpdate() > 0;
            if (deleted) {
                removeFaceProfileByEmail(normalizedEmail);
            }
            return deleted;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete app user.", e);
        }
    }

    private AppUser mapAppUser(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
        
        String userTypeStr = rs.getString("user_type");
        AppUser.UserType userType = AppUser.UserType.Buyer;
        if (userTypeStr != null) {
            for (AppUser.UserType type : AppUser.UserType.values()) {
                if (type.name().equalsIgnoreCase(userTypeStr)) {
                    userType = type;
                    break;
                }
            }
        }

        return new AppUser(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("password"),
                userType,
                createdAt,
                rs.getString("created_by_admin")
        );
    }

    public synchronized List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        users.addAll(findAllUsersInTable("admin_accounts", User.AdminType.ADMIN_ACCOUNT));
        users.addAll(findAllUsersInTable("event_manager", User.AdminType.EVENT_MANAGER));
        users.addAll(findAllUsersInTable("finance_manager", User.AdminType.FINANCE_MANAGER));
        users.sort(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER));
        return users;
    }

    public synchronized boolean updateUserCredentials(User.AdminType role, String currentEmail, String newEmail, String newPassword) {
        String normalizedCurrentEmail = normalizeEmail(currentEmail);
        String normalizedNewEmail = normalizeEmail(newEmail);

        if (!normalizedCurrentEmail.equals(normalizedNewEmail)) {
            User existingUser = findByEmail(normalizedNewEmail);
            if (existingUser != null) {
                return false;
            }
        }

        String sql = "UPDATE " + getTableName(role) + " SET email = ?, password = ? WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedNewEmail);
            statement.setString(2, newPassword);
            statement.setString(3, normalizedCurrentEmail);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update user credentials.", e);
        }
    }

    public synchronized boolean updateUserCredentialsByTarget(User.AdminType targetRole,
                                                              String targetCurrentEmail,
                                                              String newEmail,
                                                              String newPassword) {
        return updateUserCredentials(targetRole, targetCurrentEmail, newEmail, newPassword);
    }

    public synchronized boolean updateUserProfileByTarget(User.AdminType targetRole,
                                                          String targetCurrentEmail,
                                                          String newFirstName,
                                                          String newLastName,
                                                          String newEmail,
                                                          String newPassword) {
        String normalizedCurrentEmail = normalizeEmail(targetCurrentEmail);
        String normalizedNewEmail = normalizeEmail(newEmail);

        if (!normalizedCurrentEmail.equals(normalizedNewEmail)) {
            User existingUser = findByEmail(normalizedNewEmail);
            if (existingUser != null) {
                return false;
            }
        }

        String sql = "UPDATE " + getTableName(targetRole) + " SET first_name = ?, last_name = ?, email = ?, password = ? WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newFirstName == null ? "" : newFirstName.trim());
            statement.setString(2, newLastName == null ? "" : newLastName.trim());
            statement.setString(3, normalizedNewEmail);
            statement.setString(4, newPassword);
            statement.setString(5, normalizedCurrentEmail);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update user profile.", e);
        }
    }

    public synchronized boolean deleteUserByEmail(User.AdminType role, String email) {
        String normalizedEmail = normalizeEmail(email);
        String sql = "DELETE FROM " + getTableName(role) + " WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            boolean deleted = statement.executeUpdate() > 0;
            if (deleted) {
                removeFaceProfileByEmail(normalizedEmail);
            }
            return deleted;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete user.", e);
        }
    }

    public synchronized boolean upsertFaceProfile(String email, String faceSubject) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedSubject = normalizeSubject(faceSubject);
        String sql = "INSERT INTO face_id_profiles (email, face_subject, is_enabled) VALUES (?, ?, TRUE) "
                + "ON DUPLICATE KEY UPDATE face_subject = VALUES(face_subject), is_enabled = TRUE, enrolled_at = CURRENT_TIMESTAMP";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            statement.setString(2, normalizedSubject);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to save face profile.", e);
        }
    }

    public synchronized String findFaceSubjectByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String sql = "SELECT face_subject FROM face_id_profiles WHERE email = ? AND is_enabled = TRUE";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("face_subject");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to find face subject by email.", e);
        }
    }

    public synchronized String findEmailByFaceSubject(String faceSubject) {
        String normalizedSubject = normalizeSubject(faceSubject);
        String sql = "SELECT email FROM face_id_profiles WHERE face_subject = ? AND is_enabled = TRUE";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSubject);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("email");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to find email by face subject.", e);
        }
    }

    public synchronized boolean hasFaceProfileByEmail(String email) {
        return findFaceSubjectByEmail(email) != null;
    }

    public synchronized boolean removeFaceProfileByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String sql = "DELETE FROM face_id_profiles WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete face profile.", e);
        }
    }

    public synchronized boolean hasAnotherAdmin(String currentAdminEmail) {
        String normalizedEmail = normalizeEmail(currentAdminEmail);
        String sql = "SELECT COUNT(*) FROM admin_accounts WHERE email <> ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to verify admin count.", e);
        }
    }

    public synchronized List<String> findAllAdminEmails() {
        String sql = "SELECT email FROM admin_accounts";
        List<String> emails = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                emails.add(resultSet.getString("email"));
            }
            return emails;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch admin emails.", e);
        }
    }

    public synchronized int insertLoginLog(int userId, String role) {
        String sql = "INSERT INTO login_history (user_id, user_role) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, role);
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert login log.", e);
        }
    }

    public synchronized void updateLogoutTime(int logId) {
        String sql = "UPDATE login_history SET logout_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, logId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update logout time.", e);
        }
    }

    public synchronized void insertEmailLog(String sender, String recipient, String subject, String status) {
        String sql = "INSERT INTO email_logs (sender, recipient, subject, status) VALUES (?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sender);
            statement.setString(2, recipient);
            statement.setString(3, subject);
            statement.setString(4, status);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert email log.", e);
        }
    }

    // --- Statistics Queries ---

    public synchronized int getTotalUsersCount() {
        int count = 0;
        String[] tables = {"admin_accounts", "event_manager", "finance_manager", "users"};
        try (Connection connection = getConnection()) {
            for (String table : tables) {
                try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) count += rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch total users count.", e);
        }
        return count;
    }

    public synchronized int getTodayLoginsCount() {
        String sql = "SELECT COUNT(*) FROM login_history WHERE DATE(login_time) = CURDATE()";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch today logins count.", e);
        }
    }

    public synchronized int getEmailsSentTodayCount() {
        String sql = "SELECT COUNT(*) FROM email_logs WHERE DATE(sent_at) = CURDATE() AND status = 'sent'";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch today emails count.", e);
        }
    }

    public synchronized int getActiveUsersCount() {
        // Active users = users logged in today or currently logged in (no logout time)
        String sql = "SELECT COUNT(DISTINCT user_id, user_role) FROM login_history WHERE logout_time IS NULL OR DATE(login_time) = CURDATE()";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch active users count.", e);
        }
    }

    public synchronized Map<String, Integer> getRoleDistributionData() {
        Map<String, Integer> data = new HashMap<>();
        String[] tables = {"admin_accounts", "event_manager", "finance_manager"};
        String[] roles = {"Admin", "Event Manager", "Finance Manager"};
        try (Connection connection = getConnection()) {
            for (int i = 0; i < tables.length; i++) {
                try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + tables[i]);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) data.put(roles[i], rs.getInt(1));
                }
            }
            // Include user types from users table
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT user_type, COUNT(*) as cnt FROM users GROUP BY user_type");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.put(rs.getString("user_type"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch role distribution.", e);
        }
        return data;
    }

    public synchronized Map<LocalDate, Integer> getLoginActivityData() {
        Map<LocalDate, Integer> data = new TreeMap<>();
        String sql = "SELECT DATE(login_time) as date, COUNT(*) as count FROM login_history GROUP BY DATE(login_time) ORDER BY date DESC LIMIT 7";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getDate("date").toLocalDate(), rs.getInt("count"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch login activity data.", e);
        }
        return data;
    }

    public synchronized Map<LocalDate, Integer> getDailyActivityData() {
        // Combined logins and emails
        Map<LocalDate, Integer> data = new TreeMap<>();
        String sql = "SELECT d.date, (SELECT COUNT(*) FROM login_history l WHERE DATE(l.login_time) = d.date) + " +
                     "(SELECT COUNT(*) FROM email_logs e WHERE DATE(e.sent_at) = d.date) as total_activity " +
                     "FROM (SELECT DISTINCT DATE(login_time) as date FROM login_history UNION SELECT DISTINCT DATE(sent_at) FROM email_logs) d " +
                     "ORDER BY d.date DESC LIMIT 7";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date d = rs.getDate("date");
                if (d != null) {
                    data.put(d.toLocalDate(), rs.getInt("total_activity"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch daily activity data.", e);
        }
        return data;
    }

    private User findByEmailInTable(String email, String table, User.AdminType role) {
        String sql = "SELECT id, first_name, last_name, email, password FROM " + table + " WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getInt("id"),
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getString("email"),
                            resultSet.getString("password"),
                            role
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch user by email.", e);
        }
    }

    private List<User> findAllUsersInTable(String table, User.AdminType role) {
        String sql = "SELECT id, first_name, last_name, email, password FROM " + table;
        List<User> users = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new User(
                        resultSet.getInt("id"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("email"),
                        resultSet.getString("password"),
                        role
                ));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch all users.", e);
        }
    }

    private void ensureDefaultAdmin() {
        String oldEmail = "admin@bladna.local";
        String checkOldSql = "SELECT COUNT(*) FROM admin_accounts WHERE email = ?";
        String updateSql = "UPDATE admin_accounts SET email = ?, password = ? WHERE email = ?";
        String checkAnySql = "SELECT COUNT(*) FROM admin_accounts";
        String insertSql = "INSERT INTO admin_accounts (first_name, last_name, email, password) VALUES (?, ?, ?, ?)";

        try (Connection connection = getConnection()) {
            // 1. Check if old admin exists and update it
            try (PreparedStatement checkOldStmt = connection.prepareStatement(checkOldSql)) {
                checkOldStmt.setString(1, oldEmail);
                try (ResultSet rs = checkOldStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setString(1, DEFAULT_ADMIN_EMAIL);
                            updateStmt.setString(2, BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, BCrypt.gensalt()));
                            updateStmt.setString(3, oldEmail);
                            updateStmt.executeUpdate();
                            return; // Migration complete
                        }
                    }
                }
            }

            // 2. If no old admin, check if ANY admin exists
            try (PreparedStatement checkAnyStmt = connection.prepareStatement(checkAnySql);
                 ResultSet rs = checkAnyStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // At least one admin exists (maybe already updated or manually created)
                }
            }

            // 3. If no admin exists at all, insert the new default
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setString(1, DEFAULT_ADMIN_FIRST_NAME);
                insertStmt.setString(2, DEFAULT_ADMIN_LAST_NAME);
                insertStmt.setString(3, DEFAULT_ADMIN_EMAIL);
                insertStmt.setString(4, BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, BCrypt.gensalt()));
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to ensure default admin.", e);
        }
    }

    private String getTableName(User.AdminType role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        switch (role) {
            case ADMIN_ACCOUNT:
                return "admin_accounts";
            case EVENT_MANAGER:
                return "event_manager";
            case FINANCE_MANAGER:
                return "finance_manager";
            default:
                throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get database connection.", e);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeSubject(String subject) {
        return subject == null ? "" : subject.trim();
    }

}

