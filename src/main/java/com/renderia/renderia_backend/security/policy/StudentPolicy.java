package com.renderia.renderia_backend.security.policy;

/** El estudiante usa la plataforma para aprender: máximo 3 proyectos propios. */
public class StudentPolicy implements RolePolicy {

    public static final int MAX_PROJECTS = 3;

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
        return MAX_PROJECTS;
    }
}
