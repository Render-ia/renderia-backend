package com.renderia.renderia_backend.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.renderia.renderia_backend.security.policy.RolePolicies;
import com.renderia.renderia_backend.security.policy.RolePolicy;

/** Usuario que hace la petición, leído del token: su id, su rol, la política de ese rol y si es una cuenta de prueba. */
public record CurrentUser(Long id, String role, boolean demo) {

    public CurrentUser(Long id, String role) {
        this(id, role, false);
    }

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("role"),
                DemoAccounts.isDemo(jwt.getClaimAsString("email")));
    }

    public RolePolicy policy() {
        return RolePolicies.forRole(role);
    }
}
