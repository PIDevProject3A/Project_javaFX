package entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "Dali",
                "Ben Ali",
                "dali@example.com",
                "hashedPassword123",
                User.AdminType.ADMIN_ACCOUNT
        );
    }

    @Test
    @Order(1)
    void shouldCreateUserWithExpectedValues() {
        assertNotNull(user);
        assertEquals("Dali", user.getFirstName());
        assertEquals("Ben Ali", user.getLastName());
        assertEquals("dali@example.com", user.getEmail());
        assertEquals("hashedPassword123", user.getPasswordHash());
        assertEquals(User.AdminType.ADMIN_ACCOUNT, user.getAdminType());
    }

    @Test
    @Order(2)
    void shouldFormatToStringUsingEmailAndAdminType() {
        assertEquals("dali@example.com (ADMIN_ACCOUNT)", user.toString());
    }

    @Test
    @Order(3)
    void shouldSupportAllAdminTypes() {
        User eventManager = new User("A", "B", "a@b.com", "hash", User.AdminType.EVENT_MANAGER);
        User financeManager = new User("C", "D", "c@d.com", "hash", User.AdminType.FINANCE_MANAGER);

        assertEquals(User.AdminType.EVENT_MANAGER, eventManager.getAdminType());
        assertEquals(User.AdminType.FINANCE_MANAGER, financeManager.getAdminType());
    }

    @Test
    @Order(4)
    void shouldAllowNullAdminTypeWithoutThrowing() {
        User noRole = new User("No", "Role", "no@role.com", "hash", null);

        assertNull(noRole.getAdminType());
        assertEquals("no@role.com (null)", noRole.toString());
    }
}

