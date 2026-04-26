package utils;

import entities.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MyDataBase {
    private static final MyDataBase INSTANCE = new MyDataBase();
    private static final String URL = "jdbc:mysql://localhost:3306/pidevjava?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private static final String DEFAULT_ADMIN_FIRST_NAME = "Default";
    private static final String DEFAULT_ADMIN_LAST_NAME = "Admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@bladna.local";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    private MyDataBase() {
        ensureDefaultAdmin();
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

    private User findByEmailInTable(String email, String table, User.AdminType role) {
        String sql = "SELECT first_name, last_name, email, password FROM " + table + " WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
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
        String sql = "SELECT first_name, last_name, email, password FROM " + table;
        List<User> users = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new User(
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
        String checkSql = "SELECT COUNT(*) FROM admin_accounts";
        String insertSql = "INSERT INTO admin_accounts (first_name, last_name, email, password) VALUES (?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement checkStatement = connection.prepareStatement(checkSql);
             ResultSet resultSet = checkStatement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setString(1, DEFAULT_ADMIN_FIRST_NAME);
                insertStatement.setString(2, DEFAULT_ADMIN_LAST_NAME);
                insertStatement.setString(3, DEFAULT_ADMIN_EMAIL);
                insertStatement.setString(4, BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, BCrypt.gensalt()));
                insertStatement.executeUpdate();
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

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeSubject(String subject) {
        return subject == null ? "" : subject.trim();
    }

}
