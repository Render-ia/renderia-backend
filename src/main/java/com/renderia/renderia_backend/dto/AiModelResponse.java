package com.renderia.renderia_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.renderia.renderia_backend.model.AiModel;

/** { id, name, provider, modelIdentifier, isActive, isDefault } */
public record AiModelResponse(
        Long id,
        String name,
        String provider,
        String modelIdentifier,
        @JsonProperty("isActive") boolean isActive,
        @JsonProperty("isDefault") boolean isDefault) {

    public static AiModelResponse from(AiModel model) {
        return new AiModelResponse(
                model.getId(),
                model.getName(),
                model.getProvider(),
                model.getModelIdentifier(),
                model.isActive(),
                model.isDefaultModel());
    }
}
