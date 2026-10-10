package com.renderia.renderia_backend.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.renderia.renderia_backend.model.User;

/** Usuario con su rol, sin la contraseña. */
public record UserResponse(
        Long id,
        Long roleId,
        String fullName,
        String email,
        @JsonProperty("isActive") boolean isActive,
        Instant createdAt,
        RoleResponse role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getRole().getId(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                user.getCreatedAt(),
                RoleResponse.from(user.getRole()));
    }
}
