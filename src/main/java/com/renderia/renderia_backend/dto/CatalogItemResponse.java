package com.renderia.renderia_backend.dto;

import com.renderia.renderia_backend.model.CatalogEntity;

/** Lo que devuelve la API por cada fila de catálogo: { id, name, description }. */
public record CatalogItemResponse(Long id, String name, String description) {

    public static CatalogItemResponse from(CatalogEntity entity) {
        return new CatalogItemResponse(entity.getId(), entity.getName(), entity.getDescription());
    }
}
