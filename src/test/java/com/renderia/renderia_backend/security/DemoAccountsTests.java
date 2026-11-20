package com.renderia.renderia_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.renderia.renderia_backend.model.Role;

class DemoAccountsTests {

    @Test
    void thereIsOneDemoAccountPerRole() {
        assertEquals(5, DemoAccounts.ALL.size());
        for (String role : new String[] {Role.ADMIN, Role.ENGINEER, Role.REVIEWER, Role.CLIENT, Role.STUDENT}) {
            assertTrue(DemoAccounts.forRole(role).isPresent(), role);
        }
    }

    @Test
    void demoEmailsAreRecognizedIgnoringCase() {
        assertTrue(DemoAccounts.isDemo(" Admin.Prueba@renderia.co "));
        assertFalse(DemoAccounts.isDemo("admin@renderia.co"));
        assertFalse(DemoAccounts.isDemo(null));
    }
}
