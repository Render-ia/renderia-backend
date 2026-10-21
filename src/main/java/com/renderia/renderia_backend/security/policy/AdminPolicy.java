package com.renderia.renderia_backend.security.policy;

/** El administrador puede ver y gestionar todo, sin límite de proyectos. */
public class AdminPolicy implements RolePolicy {

    @Override
    public boolean canViewAllProjects() {
        return true;
    }

    @Override
    public boolean canManageAnyProject() {
        return true;
    }

    @Override
    public int projectLimit() {
        return Integer.MAX_VALUE;
    }
}
