package com.renderia.renderia_backend.security.policy;

import com.renderia.renderia_backend.model.Role;

/** Elige la estrategia que corresponde al nombre del rol. Un rol desconocido recibe la más limitada. */
public final class RolePolicies {

    private static final RolePolicy ADMIN = new AdminPolicy();
    private static final RolePolicy ENGINEER = new EngineerPolicy();
    private static final RolePolicy STUDENT = new StudentPolicy();

    private RolePolicies() {
    }

    public static RolePolicy forRole(String roleName) {
        if (Role.ADMIN.equals(roleName)) {
            return ADMIN;
        }
        if (Role.ENGINEER.equals(roleName)) {
            return ENGINEER;
        }
        return STUDENT;
    }
}
