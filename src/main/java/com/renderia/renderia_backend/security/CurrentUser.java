package com.renderia.renderia_backend.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.renderia.renderia_backend.security.policy.RolePolicies;
import com.renderia.renderia_backend.security.policy.RolePolicy;

/** Usuario que hace la petición, leído del token: su id, su rol y la política de ese rol. */
public record CurrentUser(Long id, String role) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("role"));
    }

    public RolePolicy policy() {
        return RolePolicies.forRole(role);
    }
}
