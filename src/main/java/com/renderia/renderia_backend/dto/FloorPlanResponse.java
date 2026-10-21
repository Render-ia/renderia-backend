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

    public static FloorPlanResponse from(FloorPlan plan) {
        return new FloorPlanResponse(
                plan.getId(),
                plan.getProjectId(),
                plan.getFileName(),
                plan.getFileUrl(),
                plan.getFloorLevel(),
                plan.getScale(),
                plan.getUploadedAt());
    }
}
