package com.renderia.renderia_backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Tipo de edificación (vivienda, oficina, bodega...) */
@Entity
@Table(name = "building_types")
public class BuildingType extends CatalogEntity {
}
