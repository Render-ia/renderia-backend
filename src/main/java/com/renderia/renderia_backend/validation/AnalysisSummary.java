package com.renderia.renderia_backend.validation;

import com.renderia.renderia_backend.exception.ApiException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The analysis summary (rawResponse) that the browser saves after an analysis:
 * warnings, assumptions, rooms. It must be a JSON object of bounded size, and the
 * professional sign-off inside it ("validation") is written only by the server
 * (ReviewService), so whatever the browser sends there is replaced by the stored one.
 */
public final class AnalysisSummary {

    /** About 1 MB of text: far above a real summary, small enough to protect the database. */
    public static final int MAX_CHARS = 1_000_000;
    static final String VALIDATION = "validation";

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private AnalysisSummary() {
    }

    /** Returns the summary to store: the client's JSON object with the server's validation kept as it was. */
    public static String fromClient(String incoming, String stored) {
        if (incoming.length() > MAX_CHARS) {
            throw ApiException.badRequest("El resumen del análisis es demasiado grande.");
        }
        JsonNode parsed;
        try {
            parsed = JSON.readTree(incoming);
        } catch (JacksonException e) {
            throw ApiException.badRequest("El resumen del análisis no es un JSON válido.");
        }
        if (!(parsed instanceof ObjectNode summary)) {
            throw ApiException.badRequest("El resumen del análisis debe ser un objeto JSON.");
        }
        summary.remove(VALIDATION);
        JsonNode previous = storedValidation(stored);
        if (previous != null) {
            summary.set(VALIDATION, previous);
        }
        return JSON.writeValueAsString(summary);
    }

    private static JsonNode storedValidation(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(stored);
            JsonNode validation = node == null ? null : node.get(VALIDATION);
            return validation != null && validation.isObject() ? validation : null;
        } catch (JacksonException e) {
            return null;
        }
    }
}
