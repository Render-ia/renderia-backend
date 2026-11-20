package com.renderia.renderia_backend.config;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.security.DemoAccounts;

/**
 * Creates one demo account per role (see {@link DemoAccounts}) so the teacher can try
 * the deployed platform from the login buttons. A password set in the environment is
 * used if present; otherwise a random one is generated, hashed with BCrypt and thrown
 * away, so nobody knows it and the account can only be entered through /auth/demo.
 * Disabled with DEMO_ACCOUNTS=false.
 */
@Component
@Order(10)
public class DemoAccountsInitializer implements ApplicationRunner {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountSeeder seeder;
    private final boolean enabled;
    private final Map<String, String> passwords;

    public DemoAccountsInitializer(AccountSeeder seeder,
                                   @Value("${app.demo.enabled:true}") boolean enabled,
                                   @Value("${app.demo.engineer-password:}") String engineerPassword,
                                   @Value("${app.demo.student-password:}") String studentPassword) {
        this.seeder = seeder;
        this.enabled = enabled;
        this.passwords = Map.of(Role.ENGINEER, engineerPassword, Role.STUDENT, studentPassword);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        for (DemoAccounts.Account account : DemoAccounts.ALL) {
            String password = passwords.getOrDefault(account.role(), "");
            seeder.createIfMissing(account.email(), account.fullName(), account.role(),
                    password.isBlank() ? randomPassword() : password);
        }
    }

    static String randomPassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
