package com.renderia.renderia_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;

/** Creates an account when the server starts, only if it does not exist yet. */
@Component
public class AccountSeeder {

    private static final Logger log = LoggerFactory.getLogger(AccountSeeder.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public AccountSeeder(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crea la cuenta si se dio una contraseña y el correo todavía no está registrado.
     * Nunca cambia una cuenta que ya existe.
     */
    public void createIfMissing(String email, String fullName, String roleName, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        try {
            String normalizedEmail = email.trim().toLowerCase();
            if (users.existsByEmailIgnoreCase(normalizedEmail)) {
                return;
            }
            Role role = roles.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("No existe el rol " + roleName + " en la base de datos."));
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(password.trim()));
            user.setRole(role);
            users.save(user);
            log.info("Cuenta creada: {} ({})", normalizedEmail, roleName);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la cuenta {}: {}", email, e.getMessage());
        }
    }
}
