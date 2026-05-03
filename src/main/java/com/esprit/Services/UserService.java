package com.esprit.services;

import com.esprit.entities.AppUser;
import com.esprit.entities.User;
import org.mindrot.jbcrypt.BCrypt;
import com.esprit.utils.MyDataBase;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private final MyDataBase dataBase = MyDataBase.getInstance();
    private final CompreFaceFaceIdService faceIdService = new CompreFaceFaceIdService();

    // ========== Login Result ==========

    public static class LoginResult {
        private final User adminUser;
        private final AppUser appUser;

        private LoginResult(User adminUser, AppUser appUser) {
            this.adminUser = adminUser;
            this.appUser = appUser;
        }

        public static LoginResult admin(User user) {
            return new LoginResult(user, null);
        }

        public static LoginResult user(AppUser appUser) {
            return new LoginResult(null, appUser);
        }

        public boolean isAdmin() {
            return adminUser != null;
        }

        public boolean isAppUser() {
            return appUser != null;
        }

        public User getAdminUser() {
            return adminUser;
        }

        public AppUser getAppUser() {
            return appUser;
        }
    }

    // ========== Unified Login ==========

    public LoginResult loginUnified(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            return null;
        }

        // Try admin tables first
        User adminUser = dataBase.findByEmail(email);
        if (adminUser != null) {
            String storedPassword = adminUser.getPasswordHash();
            if (verifyPassword(password, storedPassword)) {
                return LoginResult.admin(adminUser);
            }
            if (password.equals(storedPassword)) {
                String migratedHash = hashPassword(password);
                boolean migrated = dataBase.updateUserCredentials(adminUser.getAdminType(), adminUser.getEmail(), adminUser.getEmail(), migratedHash);
                if (migrated) {
                    User updated = new User(adminUser.getId(), adminUser.getFirstName(), adminUser.getLastName(), adminUser.getEmail(), migratedHash, adminUser.getAdminType());
                    return LoginResult.admin(updated);
                }
            }
        }

        // Try users table
        AppUser appUser = dataBase.findAppUserByEmail(email.trim().toLowerCase());
        if (appUser != null) {
            String storedPassword = appUser.getPasswordHash();
            if (verifyPassword(password, storedPassword)) {
                return LoginResult.user(appUser);
            }
            if (password.equals(storedPassword)) {
                String migratedHash = hashPassword(password);
                boolean migrated = dataBase.updateAppUserProfile(
                        appUser.getEmail(), appUser.getFirstName(), appUser.getLastName(),
                        appUser.getEmail(), migratedHash, appUser.getUserType());
                if (migrated) {
                    AppUser updated = new AppUser(appUser.getId(), appUser.getFirstName(), appUser.getLastName(),
                            appUser.getEmail(), migratedHash, appUser.getUserType(), appUser.getCreatedAt(), appUser.getCreatedByAdmin());
                    return LoginResult.user(updated);
                }
            }
        }

        return null;
    }

    // ========== Existing Admin Methods ==========

    public User findByEmail(String email) {
        if (isBlank(email)) {
            return null;
        }
        return dataBase.findByEmail(email.trim().toLowerCase());
    }

    public User findByFaceSubject(String faceSubject) {
        if (isBlank(faceSubject)) {
            return null;
        }
        String email = dataBase.findEmailByFaceSubject(faceSubject.trim());
        if (isBlank(email)) {
            return null;
        }
        // Check admin tables first
        User adminUser = dataBase.findByEmail(email);
        if (adminUser != null) {
            return adminUser;
        }
        return null;
    }

    public AppUser findAppUserByFaceSubject(String faceSubject) {
        if (isBlank(faceSubject)) {
            return null;
        }
        String email = dataBase.findEmailByFaceSubject(faceSubject.trim());
        if (isBlank(email)) {
            return null;
        }
        return dataBase.findAppUserByEmail(email);
    }

    public AppUser findAppUserByEmail(String email) {
        if (isBlank(email)) {
            return null;
        }
        return dataBase.findAppUserByEmail(email.trim().toLowerCase());
    }

    public String getOrCreateFaceSubject(String email) {
        if (isBlank(email)) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase();
        String existingSubject = dataBase.findFaceSubjectByEmail(normalizedEmail);
        if (!isBlank(existingSubject)) {
            return existingSubject;
        }

        String subject = "usr_" + UUID.randomUUID();
        boolean linked = dataBase.upsertFaceProfile(normalizedEmail, subject);
        return linked ? subject : null;
    }

    public boolean hasFaceId(String email) {
        if (isBlank(email)) {
            return false;
        }
        return dataBase.hasFaceProfileByEmail(email.trim().toLowerCase());
    }

    public String addOrUpdateFaceIdByAdmin(User.AdminType currentRole, String targetEmail, Path imagePath) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return "Access denied: admin role required.";
        }
        if (isBlank(targetEmail)) {
            return "Target email is required.";
        }
        if (imagePath == null) {
            return "Image requise.";
        }
        if (!faceIdService.isConfigured()) {
            return "CompreFace n'est pas configure.";
        }

        // Check both admin and app user tables
        String email = targetEmail.trim().toLowerCase();
        User adminUser = dataBase.findByEmail(email);
        AppUser appUser = dataBase.findAppUserByEmail(email);
        if (adminUser == null && appUser == null) {
            return "Target account not found.";
        }

        boolean hadFaceId = hasFaceId(email);
        String subject = getOrCreateFaceSubject(email);
        if (isBlank(subject)) {
            return "Impossible de preparer le subject Face ID.";
        }

        CompreFaceFaceIdService.EnrollResult enrollResult = faceIdService.enrollFace(subject, imagePath);
        if (!enrollResult.success()) {
            return enrollResult.message();
        }

        return hadFaceId
                ? "Face ID mis a jour avec succes pour " + email + "."
                : "Face ID ajoute avec succes pour " + email + ".";
    }

    public String deleteFaceIdByAdmin(User.AdminType currentRole, String targetEmail) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return "Access denied: admin role required.";
        }
        if (isBlank(targetEmail)) {
            return "Target email is required.";
        }

        String email = targetEmail.trim().toLowerCase();
        User adminUser = dataBase.findByEmail(email);
        AppUser appUser = dataBase.findAppUserByEmail(email);
        if (adminUser == null && appUser == null) {
            return "Target account not found.";
        }

        boolean removed = dataBase.removeFaceProfileByEmail(email);
        if (!removed) {
            return "Aucun Face ID actif a supprimer pour cet utilisateur.";
        }
        return "Face ID supprime pour " + email + ".";
    }

    public User login(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            return null;
        }

        User user = dataBase.findByEmail(email);
        if (user == null) {
            return null;
        }

        String storedPassword = user.getPasswordHash();
        if (verifyPassword(password, storedPassword)) {
            return user;
        }

        if (password.equals(storedPassword)) {
            String migratedHash = hashPassword(password);
            boolean migrated = dataBase.updateUserCredentials(user.getAdminType(), user.getEmail(), user.getEmail(), migratedHash);
            if (migrated) {
                return new User(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), migratedHash, user.getAdminType());
            }
        }
        return null;
    }

    public String createAccountByAdmin(User.AdminType currentRole,
                                       String firstName,
                                       String lastName,
                                       String email,
                                       String password,
                                       User.AdminType targetRole) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return "Only admin accounts can create new accounts.";
        }
        if (isBlank(firstName) || isBlank(lastName) || isBlank(email) || isBlank(password) || targetRole == null) {
            return "All fields are required.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email format.";
        }
        if (!hasMinLength(firstName, MIN_NAME_LENGTH) || !hasMinLength(lastName, MIN_NAME_LENGTH)) {
            return "First name and last name must be at least 3 characters.";
        }
        if (!hasMinLength(password, MIN_PASSWORD_LENGTH)) {
            return "Password must be at least 8 characters.";
        }

        String passwordHash = hashPassword(password);
        User user = new User(0, firstName.trim(), lastName.trim(), email.trim().toLowerCase(), passwordHash, targetRole);
        boolean saved = dataBase.addUser(user);
        if (!saved) {
            return "Email already exists.";
        }

        return "SUCCESS";
    }

    // ========== AppUser CRUD ==========

    public String createAppUser(User.AdminType currentRole,
                                String firstName,
                                String lastName,
                                String email,
                                String password,
                                AppUser.UserType userType) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return "Only admin accounts can create new accounts.";
        }
        if (isBlank(firstName) || isBlank(lastName) || isBlank(email) || isBlank(password) || userType == null) {
            return "All fields are required.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email format.";
        }
        if (!hasMinLength(firstName, MIN_NAME_LENGTH) || !hasMinLength(lastName, MIN_NAME_LENGTH)) {
            return "First name and last name must be at least 3 characters.";
        }
        if (!hasMinLength(password, MIN_PASSWORD_LENGTH)) {
            return "Password must be at least 8 characters.";
        }

        String passwordHash = hashPassword(password);
        String adminEmail = utils.UserSession.getCurrentUserEmail();
        AppUser appUser = new AppUser(0, firstName.trim(), lastName.trim(), email.trim().toLowerCase(),
                passwordHash, userType, null, adminEmail);
        boolean saved = dataBase.addAppUser(appUser);
        if (!saved) {
            return "Email already exists.";
        }

        return "SUCCESS";
    }

    public String updateAppUserByAdmin(User.AdminType currentRole,
                                       String targetCurrentEmail,
                                       String newFirstName,
                                       String newLastName,
                                       String newEmail,
                                       String newPassword,
                                       AppUser.UserType newUserType) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return "Only admin can modify credentials.";
        }
        if (newUserType == null
                || isBlank(targetCurrentEmail)
                || isBlank(newFirstName)
                || isBlank(newLastName)
                || isBlank(newEmail)) {
            return "All fields are required.";
        }
        if (!EMAIL_PATTERN.matcher(newEmail.trim()).matches()) {
            return "Invalid email format.";
        }
        if (!hasMinLength(newFirstName, MIN_NAME_LENGTH) || !hasMinLength(newLastName, MIN_NAME_LENGTH)) {
            return "First name and last name must be at least 3 characters.";
        }

        if (!isBlank(newPassword) && !hasMinLength(newPassword, MIN_PASSWORD_LENGTH)) {
            return "Password must be at least 8 characters.";
        }

        AppUser targetUser = dataBase.findAppUserByEmail(targetCurrentEmail);
        if (targetUser == null) {
            return "Target account not found.";
        }

        String passwordToStore = isBlank(newPassword) ? targetUser.getPasswordHash() : hashPassword(newPassword);

        boolean updated = dataBase.updateAppUserProfile(
                targetCurrentEmail,
                newFirstName,
                newLastName,
                newEmail,
                passwordToStore,
                newUserType
        );
        if (!updated) {
            return "Unable to update profile.";
        }
        return "SUCCESS";
    }

    public boolean deleteAppUserByAdmin(String targetEmail) {
        if (isBlank(targetEmail)) {
            return false;
        }
        return dataBase.deleteAppUserByEmail(targetEmail);
    }

    public List<AppUser> getAllAppUsers() {
        return dataBase.findAllAppUsers();
    }

    // ========== Existing Admin Methods (continued) ==========

    public String updateCredentials(User.AdminType role, String currentEmail, String newEmail, String newPassword) {
        if (role == null || isBlank(currentEmail) || isBlank(newEmail) || isBlank(newPassword)) {
            return "All fields are required.";
        }
        if (!EMAIL_PATTERN.matcher(newEmail.trim()).matches()) {
            return "Invalid email format.";
        }

        if (!hasMinLength(newPassword, MIN_PASSWORD_LENGTH)) {
            return "Password must be at least 8 characters.";
        }

        boolean updated = dataBase.updateUserCredentials(role, currentEmail, newEmail, hashPassword(newPassword));
        if (!updated) {
            return "Unable to update credentials.";
        }
        return "SUCCESS";
    }

    public List<User> getUsersEditableByCurrentUser(User.AdminType currentRole) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return Collections.emptyList();
        }
        return dataBase.findAllUsers();
    }

    public List<User> getOtherUsersEditableByCurrentUser(User.AdminType currentRole, String currentUserEmail) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return Collections.emptyList();
        }
        List<User> allUsers = dataBase.findAllUsers();
        if (currentUserEmail == null) return allUsers;
        
        allUsers.removeIf(user -> user.getEmail().equalsIgnoreCase(currentUserEmail));
        return allUsers;
    }

    public String updateCredentialsByAdmin(User.AdminType currentRole,
                                           String targetCurrentEmail,
                                           User.AdminType targetRole,
                                           String newFirstName,
                                           String newLastName,
                                           String newEmail,
                                           String newPassword) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return "Only admin can modify credentials.";
        }
        if (targetRole == null
                || isBlank(targetCurrentEmail)
                || isBlank(newFirstName)
                || isBlank(newLastName)
                || isBlank(newEmail)) {
            return "All fields are required.";
        }
        if (!EMAIL_PATTERN.matcher(newEmail.trim()).matches()) {
            return "Invalid email format.";
        }
        if (!hasMinLength(newFirstName, MIN_NAME_LENGTH) || !hasMinLength(newLastName, MIN_NAME_LENGTH)) {
            return "First name and last name must be at least 3 characters.";
        }

        if (!isBlank(newPassword) && !hasMinLength(newPassword, MIN_PASSWORD_LENGTH)) {
            return "Password must be at least 8 characters.";
        }

        User targetUser = dataBase.findByEmail(targetCurrentEmail);
        if (targetUser == null) {
            return "Target account not found.";
        }

        String passwordToStore = isBlank(newPassword) ? targetUser.getPasswordHash() : hashPassword(newPassword);

        boolean updated = dataBase.updateUserProfileByTarget(
                targetRole,
                targetCurrentEmail,
                newFirstName,
                newLastName,
                newEmail,
                passwordToStore
        );
        if (!updated) {
            return "Unable to update profile.";
        }
        return "SUCCESS";
    }

    public boolean deleteAccount(User.AdminType role, String email) {
        if (role == null || isBlank(email)) {
            return false;
        }

        if (role == User.AdminType.ADMIN_ACCOUNT && !dataBase.hasAnotherAdmin(email)) {
            return false;
        }

        return dataBase.deleteUserByEmail(role, email);
    }

    public boolean deleteAccountByAdmin(String targetEmail) {
        if (isBlank(targetEmail)) {
            return false;
        }

        User targetUser = dataBase.findByEmail(targetEmail);
        if (targetUser == null) {
            return false;
        }

        if (targetUser.getAdminType() == User.AdminType.ADMIN_ACCOUNT && !dataBase.hasAnotherAdmin(targetUser.getEmail())) {
            return false;
        }

        return dataBase.deleteUserByEmail(targetUser.getAdminType(), targetUser.getEmail());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasMinLength(String value, int minLength) {
        return value != null && value.trim().length() >= minLength;
    }

    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    private boolean verifyPassword(String plainPassword, String storedPassword) {
        if (isBlank(storedPassword)) {
            return false;
        }
        if (!isBcryptHash(storedPassword)) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, storedPassword);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}

