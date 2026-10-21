package com.renderia.renderia_backend.dto;

/** Cambios de un análisis; los campos ausentes no cambian. */
public record AnalysisUpdateRequest(String status, String rawResponse, String errorMessage, Integer durationMs) {
}
