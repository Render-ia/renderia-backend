package com.renderia.renderia_backend.dto;

import java.time.Instant;

import com.renderia.renderia_backend.model.Model3D;

public record Model3DResponse(
        Long id,
        Long floorPlanId,
        Long aiAnalysisId,
        String fileUrl,
        String format,
        int elementCount,
        Instant createdAt) {

    public static Model3DResponse from(Model3D m) {
        return new Model3DResponse(m.getId(), m.getFloorPlanId(), m.getAiAnalysisId(), m.getFileUrl(),
                m.getFormat(), m.getElementCount(), m.getCreatedAt());
    }
}
