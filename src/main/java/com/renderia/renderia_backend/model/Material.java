package com.renderia.renderia_backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Material de un elemento estructural (concreto, ladrillo, acero...) */
@Entity
@Table(name = "materials")
public class Material extends CatalogEntity {
}
