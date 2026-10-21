package com.renderia.renderia_backend.dto;

/** Cuerpo de POST /ai/analyze. Si no llega la escala, se usa la del plano. */
public record AiAnalyzeRequest(Long floorPlanId, Long aiModelId, String scale) {
}
