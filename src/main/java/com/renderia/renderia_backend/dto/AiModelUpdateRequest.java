package com.renderia.renderia_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo de PUT /ai-models/{id}: { isActive?, isDefault? }. Los campos ausentes no cambian. */
public record AiModelUpdateRequest(
        @JsonProperty("isActive") Boolean isActive,
        @JsonProperty("isDefault") Boolean isDefault) {
}
