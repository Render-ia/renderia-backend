package com.renderia.renderia_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Rol de usuario: Administrador, Ingeniero o Estudiante. */
@Entity
@Table(name = "roles")
public class Role {

    public static final String ADMIN = "Administrador";
    public static final String ENGINEER = "Ingeniero";
    public static final String STUDENT = "Estudiante";

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
