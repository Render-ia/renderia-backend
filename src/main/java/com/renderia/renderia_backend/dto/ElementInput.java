package com.renderia.renderia_backend.dto;

/** Elemento para guardar en un análisis. Medidas en metros; confianza entre 0 y 1. */
public record ElementInput(
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
}
