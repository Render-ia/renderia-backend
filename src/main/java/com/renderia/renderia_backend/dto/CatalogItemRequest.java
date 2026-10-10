package com.renderia.renderia_backend.dto;

/** Cuerpo de POST y PUT de un catálogo: { name, description }. */
public record CatalogItemRequest(String name, String description) {
}
