package com.renderia.renderia_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Modelo de IA disponible para analizar planos. Marcar otro modelo como
 * predeterminado cambia la IA que se usa sin tocar el código (el "switch").
 */
@Entity
@Table(name = "ai_models")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "model_identifier", nullable = false, unique = true, length = 150)
    private String modelIdentifier;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_default", nullable = false)
    private boolean defaultModel;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelIdentifier() {
        return modelIdentifier;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(boolean defaultModel) {
        this.defaultModel = defaultModel;
    }
}
