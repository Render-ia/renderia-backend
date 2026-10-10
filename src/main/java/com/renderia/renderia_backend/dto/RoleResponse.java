package com.renderia.renderia_backend.dto;

import com.renderia.renderia_backend.model.Role;

public record RoleResponse(Long id, String name, String description) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription());
    }
}
