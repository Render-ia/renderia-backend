package com.renderia.renderia_backend.security;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.renderia.renderia_backend.model.Role;

/**
 * Demo accounts, one per role, for the teacher and the jury. They are entered
 * with the "Usar" buttons of the login through /auth/demo, so no password ever
 * reaches the browser. Their passwords are random unless set in environment
 * variables, and are only stored as BCrypt hashes.
 *
 * Demo accounts are read-only outside their own data: the demo administrator
 * sees the whole admin panel but cannot change users, catalogs or AI models,
 * nor other people's projects.
 */
public final class DemoAccounts {

    public record Account(String role, String email, String fullName) {
    }

    public static final List<Account> ALL = List.of(
            new Account(Role.ADMIN, "admin.prueba@renderia.co", "Administrador de prueba"),
            new Account(Role.ENGINEER, "ingeniero@renderia.co", "Ingeniero de prueba"),
            new Account(Role.REVIEWER, "revisor@renderia.co", "Revisor de prueba"),
            new Account(Role.CLIENT, "cliente@renderia.co", "Cliente de prueba"),
            new Account(Role.STUDENT, "estudiante@renderia.co", "Estudiante de prueba"));

    /** Authority added to demo sessions, so security rules can block their admin writes. */
    public static final String AUTHORITY = "DEMO_ACCOUNT";

    private DemoAccounts() {
    }

    public static boolean isDemo(String email) {
        if (email == null) {
            return false;
        }
        String clean = email.trim().toLowerCase(Locale.ROOT);
        return ALL.stream().anyMatch(account -> account.email().equals(clean));
    }

    public static Optional<Account> forRole(String role) {
        return ALL.stream().filter(account -> account.role().equals(role)).findFirst();
    }

    public static String emailOf(String role) {
        return forRole(role).map(Account::email).orElseThrow();
    }
}
