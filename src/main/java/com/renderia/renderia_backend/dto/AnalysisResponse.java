package com.renderia.renderia_backend.dto;

import java.time.Instant;

import com.renderia.renderia_backend.model.AiAnalysis;

public record AnalysisResponse(
        Long id,
        Long floorPlanId,
        Long aiModelId,
        String status,
        String rawResponse,
        String errorMessage,
        Integer durationMs,
        Instant createdAt) {

    public static AnalysisResponse from(AiAnalysis a) {
        return new AnalysisResponse(a.getId(), a.getFloorPlanId(), a.getAiModelId(), a.getStatus().name(),
                a.getRawResponse(), a.getErrorMessage(), a.getDurationMs(), a.getCreatedAt());
    }
}
