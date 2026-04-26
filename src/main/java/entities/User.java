package entities;

public class User {
    public enum AdminType {
        ADMIN_ACCOUNT,
        EVENT_MANAGER,
        FINANCE_MANAGER
    }

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String passwordHash;
    private final AdminType adminType;

    public User(String firstName, String lastName, String email, String passwordHash, AdminType adminType) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.adminType = adminType;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AdminType getAdminType() {
        return adminType;
    }

    @Override
    public String toString() {
        return email + " (" + adminType + ")";
    }
}

