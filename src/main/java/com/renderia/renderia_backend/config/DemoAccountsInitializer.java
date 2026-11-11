package com.renderia.renderia_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.model.Role;

/**
 * Cuentas de prueba de Ingeniero y Estudiante, para que el profesor pueda
 * entrar a la plataforma desplegada. Las contraseñas vienen de las variables
 * DEMO_ENGINEER_PASSWORD y DEMO_STUDENT_PASSWORD; si no existen, no se crea nada.
 * No hay cuenta de prueba de administrador a propósito: con ella cualquiera
 * podría gestionar usuarios y catálogos.
 */
@Component
public class DemoAccountsInitializer implements ApplicationRunner {

    public static final String ENGINEER_EMAIL = "ingeniero@renderia.co";
    public static final String STUDENT_EMAIL = "estudiante@renderia.co";

    private final AccountSeeder seeder;
    private final String engineerPassword;
    private final String studentPassword;

    public DemoAccountsInitializer(AccountSeeder seeder,
                                   @Value("${app.demo.engineer-password}") String engineerPassword,
                                   @Value("${app.demo.student-password}") String studentPassword) {
        this.seeder = seeder;
        this.engineerPassword = engineerPassword;
        this.studentPassword = studentPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seeder.createIfMissing(ENGINEER_EMAIL, "Ingeniero de prueba", Role.ENGINEER, engineerPassword);
        seeder.createIfMissing(STUDENT_EMAIL, "Estudiante de prueba", Role.STUDENT, studentPassword);
    }
}
