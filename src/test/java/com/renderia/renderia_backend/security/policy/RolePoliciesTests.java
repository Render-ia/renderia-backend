package com.renderia.renderia_backend.security.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.renderia.renderia_backend.model.Role;

/** Patrón Strategy: cada rol recibe su política y un rol desconocido la más limitada. */
class RolePoliciesTests {

    @Test
    void adminSeesAndManagesEverything() {
        RolePolicy policy = RolePolicies.forRole(Role.ADMIN);
        assertInstanceOf(AdminPolicy.class, policy);
        assertTrue(policy.canViewAllProjects());
        assertTrue(policy.canManageAnyProject());
    }

    @Test
    void engineerOnlyWorksOnOwnProjectsWithoutLimit() {
        RolePolicy policy = RolePolicies.forRole(Role.ENGINEER);
        assertFalse(policy.canViewAllProjects());
        assertFalse(policy.canManageAnyProject());
        assertEquals(Integer.MAX_VALUE, policy.projectLimit());
    }

    @Test
    void studentHasAProjectLimit() {
        RolePolicy policy = RolePolicies.forRole(Role.STUDENT);
        assertFalse(policy.canManageAnyProject());
        assertEquals(StudentPolicy.MAX_PROJECTS, policy.projectLimit());
    }

    @Test
    void unknownRoleGetsTheMostLimitedPolicy() {
        assertInstanceOf(StudentPolicy.class, RolePolicies.forRole("Hacker"));
        assertInstanceOf(StudentPolicy.class, RolePolicies.forRole(null));
    }
}
