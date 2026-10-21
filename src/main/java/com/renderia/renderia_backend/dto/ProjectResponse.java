package com.renderia.renderia_backend.dto;

import java.time.Instant;

import com.renderia.renderia_backend.model.Project;

public record ProjectResponse(
        Long id,
        Long userId,
        Long buildingTypeId,
        String name,
        String description,
        String location,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getUserId(),
                project.getBuildingTypeId(),
                project.getName(),
                project.getDescription() == null ? "" : project.getDescription(),
                project.getLocation() == null ? "" : project.getLocation(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
