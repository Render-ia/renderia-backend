package com.renderia.renderia_backend.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.support.TestUsers;

class AccountSeederTests {

    @Test
    void anOldPublishedDemoPasswordStopsWorkingOnStart() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder(4);
        User engineer = TestUsers.user(2L, Role.ENGINEER, true);
        engineer.setPasswordHash(encoder.encode("Ingeniero123*"));
        when(users.findByEmailIgnoreCase("ingeniero@renderia.co")).thenReturn(Optional.of(engineer));

        String fresh = DemoAccountsInitializer.randomPassword();
        new AccountSeeder(users, mock(RoleRepository.class), encoder)
                .ensureDemoAccount("ingeniero@renderia.co", "Ingeniero de prueba", Role.ENGINEER, fresh);

        verify(users).save(engineer);
        assertFalse(encoder.matches("Ingeniero123*", engineer.getPasswordHash()));
        assertTrue(engineer.getPasswordHash().startsWith("$2"));
        assertNotEquals(fresh, engineer.getPasswordHash());
    }

    @Test
    void randomPasswordsAreLongAndDifferent() {
        String one = DemoAccountsInitializer.randomPassword();
        assertTrue(one.length() >= 40);
        assertNotEquals(one, DemoAccountsInitializer.randomPassword());
    }
}
