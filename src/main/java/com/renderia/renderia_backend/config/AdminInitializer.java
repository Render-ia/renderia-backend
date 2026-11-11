package com.renderia.renderia_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.model.Role;

/**
 * Crea la primera cuenta de administrador al arrancar, si se definieron
 * ADMIN_EMAIL y ADMIN_PASSWORD y esa cuenta todavía no existe.
 * Así ninguna contraseña queda escrita en el código.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private final AccountSeeder seeder;
    private final String email;
    private final String password;

    public AdminInitializer(AccountSeeder seeder,
                            @Value("${app.admin.email}") String email,
                            @Value("${app.admin.password}") String password) {
        this.seeder = seeder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        seeder.createIfMissing(email, "Administrador Render.IA", Role.ADMIN, password);
    }
}
