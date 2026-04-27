package services;

import entities.User;
import org.mindrot.jbcrypt.BCrypt;
import utils.MyDataBase;

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
        return dataBase.findByEmail(email);
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

        User targetUser = findByEmail(targetEmail);
        if (targetUser == null) {
            return "Target account not found.";
        }

        boolean hadFaceId = hasFaceId(targetUser.getEmail());
        String subject = getOrCreateFaceSubject(targetUser.getEmail());
        if (isBlank(subject)) {
            return "Impossible de preparer le subject Face ID.";
        }

        CompreFaceFaceIdService.EnrollResult enrollResult = faceIdService.enrollFace(subject, imagePath);
        if (!enrollResult.success()) {
            return enrollResult.message();
        }

        return hadFaceId
                ? "Face ID mis a jour avec succes pour " + targetUser.getEmail() + "."
                : "Face ID ajoute avec succes pour " + targetUser.getEmail() + ".";
    }

    public String deleteFaceIdByAdmin(User.AdminType currentRole, String targetEmail) {
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            return "Access denied: admin role required.";
        }
        if (isBlank(targetEmail)) {
            return "Target email is required.";
        }

        User targetUser = findByEmail(targetEmail);
        if (targetUser == null) {
            return "Target account not found.";
        }

        boolean removed = dataBase.removeFaceProfileByEmail(targetUser.getEmail());
        if (!removed) {
            return "Aucun Face ID actif a supprimer pour cet utilisateur.";
        }
        return "Face ID supprime pour " + targetUser.getEmail() + ".";
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

    private String hashPassword(String plainPassword) {
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
