package com.renderia.renderia_backend.dto;

import java.util.List;
import java.util.Map;

/** Respuesta del servicio de IA (POST /analyze de renderia-ai). */
public record AiServiceResponse(
        String provider,
        String model,
        Long durationMs,
        List<DetectedElement> elements,
        Map<String, Object> summary) {
}
