package com.renderia.renderia_backend.support;

import org.springframework.test.util.ReflectionTestUtils;

import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;

/** Usuarios de prueba sin base de datos (los ids se asignan por reflexión, como lo haría JPA). */
public final class TestUsers {

    private TestUsers() {
    }

    public static Role role(long id, String name) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", id);
        ReflectionTestUtils.setField(role, "name", name);
        return role;
    }

    public static User user(long id, String roleName, boolean active) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setRole(role(id + 100, roleName));
        user.setFullName("Usuario " + id);
        user.setEmail("usuario" + id + "@renderia.co");
        user.setActive(active);
        return user;
    }
}
