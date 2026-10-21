package com.renderia.renderia_backend.dto;

import com.renderia.renderia_backend.model.StructuralElement;

public record ElementResponse(
        Long id,
        Long aiAnalysisId,
        Long elementTypeId,
        Long materialId,
        Double startX,
        Double startY,
        Double endX,
        Double endY,
        Double width,
        Double height,
        Double thickness,
        Double confidence) {

    public static ElementResponse from(StructuralElement e) {
        return new ElementResponse(e.getId(), e.getAiAnalysisId(), e.getElementTypeId(), e.getMaterialId(),
                e.getStartX(), e.getStartY(), e.getEndX(), e.getEndY(), e.getWidth(), e.getHeight(),
                e.getThickness(), e.getConfidence());
    }
}
