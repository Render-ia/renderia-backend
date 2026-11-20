package com.renderia.renderia_backend.security.policy;

import com.renderia.renderia_backend.model.Role;

/** Elige la estrategia que corresponde al nombre del rol. Un rol desconocido recibe la de Estudiante (3 proyectos propios). */
public final class RolePolicies {

    private static final RolePolicy ADMIN = new AdminPolicy();
    private static final RolePolicy ENGINEER = new EngineerPolicy();
    private static final RolePolicy STUDENT = new StudentPolicy();
    private static final RolePolicy REVIEWER = new ReviewerPolicy();
    private static final RolePolicy CLIENT = new ClientPolicy();

    private RolePolicies() {
    }

    public static RolePolicy forRole(String roleName) {
        if (Role.ADMIN.equals(roleName)) {
            return ADMIN;
        }
        if (Role.ENGINEER.equals(roleName)) {
            return ENGINEER;
        }
        if (Role.REVIEWER.equals(roleName)) {
            return REVIEWER;
        }
        if (Role.CLIENT.equals(roleName)) {
            return CLIENT;
        }
        return STUDENT;
    }
}
