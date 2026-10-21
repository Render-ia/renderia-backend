package com.renderia.renderia_backend.security.policy;

/** El ingeniero trabaja en sus propios proyectos, sin límite. */
public class EngineerPolicy implements RolePolicy {

    @Override
    public boolean canViewAllProjects() {
        return false;
    }

    @Override
    public boolean canManageAnyProject() {
        return false;
    }

    @Override
    public int projectLimit() {
        return Integer.MAX_VALUE;
    }
}
