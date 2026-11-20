package com.renderia.renderia_backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Prices, parameters and AIU the engineer set for the budget of one analysis. */
@Entity
@Table(name = "budget_settings")
public class BudgetSettings {

    @Id
    @Column(name = "ai_analysis_id")
    private Long aiAnalysisId;

    /** JSON object with "params", "prices" and "aiu", each a map of names to numbers. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String inputs;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BudgetSettings() {
    }

    public BudgetSettings(Long aiAnalysisId) {
        this.aiAnalysisId = aiAnalysisId;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getAiAnalysisId() {
        return aiAnalysisId;
    }

    public String getInputs() {
        return inputs;
    }

    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
