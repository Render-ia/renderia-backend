package com.renderia.renderia_backend.config;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.security.DemoAccounts;

/**
 * Prepares one demo account per role (see {@link DemoAccounts}) so the teacher can try
 * the deployed platform from the login buttons. Each start gives them a new random
 * password, hashed with BCrypt and thrown away: nobody knows it, it is not in any
 * repository or log, and the accounts can only be entered through /auth/demo.
 * Disabled with DEMO_ACCOUNTS=false.
 */
@Component
@Order(10)
public class DemoAccountsInitializer implements ApplicationRunner {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountSeeder seeder;
    private final boolean enabled;

    public DemoAccountsInitializer(AccountSeeder seeder, @Value("${app.demo.enabled:true}") boolean enabled) {
        this.seeder = seeder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        for (DemoAccounts.Account account : DemoAccounts.ALL) {
            seeder.ensureDemoAccount(account.email(), account.fullName(), account.role(), randomPassword());
        }
    }

    static String randomPassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
