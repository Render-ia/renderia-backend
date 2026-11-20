package com.renderia.renderia_backend.validation;

import java.util.Map;

import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.StructuralElement;

/**
 * Measures a person may correct on a detected element: height, thickness and
 * width, in meters. Coordinates are never edited here; they come from the plan.
 */
public final class ElementMeasures {

    public static final double MIN_METERS = 0.01;
    public static final double MAX_METERS = 50;

    private static final Map<String, String> LABELS = Map.of(
            "height", "La altura",
            "thickness", "El espesor",
            "width", "El ancho");

    private ElementMeasures() {
    }

    /** Applies the measures present in the request; a null value clears the measure. */
    public static void apply(StructuralElement element, Map<String, Object> changes) {
        if (changes.containsKey("height")) {
            element.setHeight(parse(changes.get("height"), LABELS.get("height")));
        }
        if (changes.containsKey("thickness")) {
            element.setThickness(parse(changes.get("thickness"), LABELS.get("thickness")));
        }
        if (changes.containsKey("width")) {
            element.setWidth(parse(changes.get("width"), LABELS.get("width")));
        }
    }

    /** Reads a value in meters, rounded to the millimeter, or rejects it with a message for the user. */
    public static Double parse(Object value, String label) {
        if (value == null) {
            return null;
        }
        double meters;
        if (value instanceof Number number) {
            meters = number.doubleValue();
        } else {
            try {
                meters = Double.parseDouble(String.valueOf(value).trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw ApiException.badRequest(label + " debe ser un número en metros.");
            }
        }
        if (!Double.isFinite(meters) || meters < MIN_METERS || meters > MAX_METERS) {
            throw ApiException.badRequest(label + " debe estar entre 0,01 y 50 metros.");
        }
        return Math.round(meters * 1000) / 1000.0;
    }
}
