package com.renderia.renderia_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;

/**
 * Crea la primera cuenta de administrador al arrancar, si se definieron
 * ADMIN_EMAIL y ADMIN_PASSWORD y esa cuenta todavía no existe.
 * Así ninguna contraseña queda escrita en el código.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.email}") String email,
                            @Value("${app.admin.password}") String password) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        try {
            String normalized = email.trim().toLowerCase();
            if (users.existsByEmailIgnoreCase(normalized)) {
                return;
            }
            Role admin = roles.findByName(Role.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("No existe el rol Administrador en la base de datos."));
            User user = new User();
            user.setFullName("Administrador Render.IA");
            user.setEmail(normalized);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(admin);
            users.save(user);
            log.info("Cuenta de administrador creada: {}", normalized);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la cuenta de administrador inicial: {}", e.getMessage());
        }
    }
}
