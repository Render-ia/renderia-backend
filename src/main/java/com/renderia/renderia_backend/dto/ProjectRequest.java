package com.renderia.renderia_backend.dto;

/** Cuerpo de POST y PUT /projects. El dueño se toma del token, no del cuerpo. */
public record ProjectRequest(String name, String description, String location, Long buildingTypeId) {
}
