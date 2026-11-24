package com.renderia.renderia_backend.dto;

/**
 * Cuerpo de POST /api/v1/analyses/run (crear y correr en una sola llamada)
 * y de POST /api/v1/analyses/{id}/run (donde floorPlanId y aiModelId se ignoran).
 * La escala es opcional: si no llega se usa la guardada en el plano.
 */
public record AnalysisRunRequest(Long floorPlanId, Long aiModelId, String scale) {
}
