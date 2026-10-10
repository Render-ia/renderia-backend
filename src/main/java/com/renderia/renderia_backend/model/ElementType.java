package com.renderia.renderia_backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Tipo de elemento estructural que la IA reconoce (muro, columna, viga...) */
@Entity
@Table(name = "element_types")
public class ElementType extends CatalogEntity {
}
