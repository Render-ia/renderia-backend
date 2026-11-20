package com.renderia.renderia_backend.security.policy;

/**
 * The client of a project only sees the projects that their owners shared
 * with them, read-only. They do not create projects.
 */
public class ClientPolicy implements RolePolicy {

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
        return 0;
    }
}
