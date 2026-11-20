package com.renderia.renderia_backend.dto;

import java.time.Instant;

import com.renderia.renderia_backend.model.FloorPlan;

public record FloorPlanResponse(
        Long id,
        Long projectId,
        String fileName,
        String fileUrl,
        int floorLevel,
        String scale,
        Instant uploadedAt) {

    /** imageUrl es el enlace firmado de FloorPlanImageSigner, no el que está guardado en la tabla. */
    public static FloorPlanResponse from(FloorPlan plan, String imageUrl) {
        return new FloorPlanResponse(
                plan.getId(),
                plan.getProjectId(),
                plan.getFileName(),
                imageUrl,
                plan.getFloorLevel(),
                plan.getScale(),
                plan.getUploadedAt());
    }
}
