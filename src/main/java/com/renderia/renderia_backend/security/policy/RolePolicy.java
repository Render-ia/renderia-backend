package com.renderia.renderia_backend.security.policy;

/**
 * Qué puede hacer cada rol.
 *
 * Patrón Strategy: cada rol es una estrategia distinta (AdminPolicy, EngineerPolicy,
 * StudentPolicy) y los servicios le preguntan a la política sin usar if por rol.
 * Es la misma idea que usa el frontend en src/auth/policies.ts.
 */
public interface RolePolicy {

    /** Puede ver los proyectos de todos los usuarios. */
    boolean canViewAllProjects();

    /** Puede editar o borrar proyectos de otros usuarios. */
    boolean canManageAnyProject();

    /** Máximo de proyectos propios; Integer.MAX_VALUE si no tiene límite. */
    int projectLimit();
}
