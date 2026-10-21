package com.renderia.renderia_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo de PUT /users/{id}: { roleId?, isActive? }. */
public record UserUpdateRequest(Long roleId, @JsonProperty("isActive") Boolean isActive) {
}
