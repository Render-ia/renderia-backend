package com.renderia.renderia_backend.dto;

/** Elemento tal como lo devuelve el servicio de IA: con nombres en lugar de ids. */
public record DetectedElement(
        String type,
        String material,
        Double startX,
        Double startY,
        Double endX,
        Double endY,
        Double width,
        Double height,
        Double thickness,
        Double confidence) {
}
